package com.shin.chat.jwt;

import com.shin.chat.exception.InvalidTokenException;
import com.shin.chat.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtManager {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.token.access-expiration}") // 86400000 (1000 * 60 * 60 * 24 1일)
    private long ACCESS_TOKEN_EXPIRATION_TIME;

    @Value("${jwt.token.refresh-expiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    private Key key;

    // @Value 주입 완료 후 key 초기화 (필드 초기화 시점에는 SECRET_KEY가 null이므로 @PostConstruct 사용)
    @PostConstruct
    private void init() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // AccessToken 생성 (만료: 1일)
    public String createAccessToken(String username) {
        return buildToken(username, ACCESS_TOKEN_EXPIRATION_TIME);
    }

    // RefreshToken 생성 (만료: 5일)
    public String createRefreshToken(String username) {
        return buildToken(username, REFRESH_TOKEN_EXPIRATION_TIME);
    }

    private String buildToken(String username, long expirationTime) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT") // 토큰 타입. 명시하지 않아도 디폴트 적용.
                .setSubject(username)         // 토큰 사용자
                .setIssuedAt(new Date())      // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))  // 만료 시간
                .signWith(key)                // key 객체로 서명. 알고리즘 명시 방식은 deprecated.
                .compact();
    }

    // 토큰 복호화
    private Claims getClaims(String token) {
        return Jwts.parserBuilder() // parser 방식 deprecated. parserBuilder() 사용.
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)  // 토큰 형식, 서명, 만료 시간 검증, 파싱 수행
                .getBody();
    }

    // 토큰 유효성 검증
    public void validateToken(String token) {
        try {
            getClaims(token);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }
    }

    // username 추출
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

}