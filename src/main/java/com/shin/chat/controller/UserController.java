package com.shin.chat.controller;

import com.shin.chat.domain.dto.LoginRequestDto;
import com.shin.chat.domain.dto.LoginResponseDto;
import com.shin.chat.domain.dto.RefreshTokenRequestDto;
import com.shin.chat.redis.RedisTokenService;
import com.shin.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RedisTokenService redisTokenService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody LoginRequestDto request) {
        userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        redisTokenService.deleteRefreshToken(username);
        return ResponseEntity.ok().build();
    }

}
