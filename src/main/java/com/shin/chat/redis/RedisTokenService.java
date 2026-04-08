package com.shin.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// CrudRepository(@RedisHash) 미사용 이유:
// CrudRepository는 내부적으로 Hash 자료구조(HSET)를 사용하며, 여러 필드를 가진 엔티티를
// 다양한 조건으로 조회(findByXxx)할 때 유리하다.
// RefreshToken은 username → token 문자열 단일 key-value 구조이며,
// 조회 방식도 username 단일 키 조회만 필요하므로 RedisTemplate.opsForValue()로 충분하다고 판단된다.

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.token.refresh-expiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    // RefreshToken 저장 (TTL: refresh-expiration 설정값)
    public void saveRefreshToken(String username, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + username,
                refreshToken,
                REFRESH_TOKEN_EXPIRATION_TIME,
                TimeUnit.MILLISECONDS
        );
    }

    // RefreshToken 조회
    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
    }

    // RefreshToken 삭제 (로그아웃 시 사용)
    public void deleteRefreshToken(String username) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
    }
}