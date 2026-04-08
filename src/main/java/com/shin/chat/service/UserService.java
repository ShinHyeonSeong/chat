package com.shin.chat.service;

import com.shin.chat.domain.dto.LoginRequestDto;
import com.shin.chat.domain.dto.LoginResponseDto;
import com.shin.chat.domain.dto.RefreshTokenRequestDto;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.domain.mapper.UserMapper;
import com.shin.chat.exception.DuplicateUsernameException;
import com.shin.chat.exception.InvalidPasswordException;
import com.shin.chat.exception.InvalidTokenException;
import com.shin.chat.exception.TokenExpiredException;
import com.shin.chat.exception.UserNotFoundException;
import com.shin.chat.jwt.JwtManager;
import com.shin.chat.redis.RedisTokenService;
import com.shin.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtManager jwtManager;
    private final RedisTokenService redisTokenService;
    private final UserMapper userMapper;

    // 회원 가입
    public void signup(LoginRequestDto request) {

        // 1. 중복 아이디 확인
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateUsernameException();

        // 2. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        userRepository.save(new UserEntity(request.getUsername(), encodedPassword));
    }

    // 로그인
    public LoginResponseDto login(LoginRequestDto request) {

        // 1. 유저 조회
        UserEntity user = getUserByUsername(request.getUsername());
        log.info("UserService Login. {}", user.getUsername());

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 3. JWT 생성
        String accessToken = jwtManager.createAccessToken(user.getUsername());
        String refreshToken = jwtManager.createRefreshToken(user.getUsername());

        // 4. RefreshToken Redis 저장
        redisTokenService.saveRefreshToken(user.getUsername(), refreshToken);
        log.info("Refresh Token Redis 저장. {}", refreshToken);

        // 5. 반환
        return new LoginResponseDto(accessToken, refreshToken);
    }

    // RefreshToken 검증 후 AccessToken 재발급
    public LoginResponseDto refresh(RefreshTokenRequestDto request) {
        log.info("refresh service 진입");
        String refreshToken = request.getRefreshToken();

        log.info("토큰 검증");
        // 1. 토큰 유효성 검증
        jwtManager.validateToken(refreshToken);

        // 2. username 추출
        String username = jwtManager.getUsername(refreshToken);

        // 3. Redis 저장값과 일치 여부 확인
        String storedUser = redisTokenService.getRefreshToken(username);
        if (storedUser == null)
            throw new TokenExpiredException();
        if (!refreshToken.equals(storedUser))
            throw new InvalidTokenException();

        // 4. AccessToken 발급
        String newAccessToken = jwtManager.createAccessToken(username);
        log.info("access token 재발급");

        return new LoginResponseDto(newAccessToken, refreshToken);
    }

    // username으로 user 검색
    private UserEntity getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
    }

}
