package com.shin.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisUnreadService {

    private final RedisTemplate<String, String> redisTemplate;

    // Key: "unread:{userId}:{roomId}" value : 안읽음 메시지 수
    private static final String PREFIX = "unread:";
    // 30일 뒤 자동 만료 <- 만료 시점을 설정했으므로, 캐싱 전략이 필요할 것
    private static final long TTL_DAYS = 30;

    private String key(Long userId, Long roomId) {
        return PREFIX + userId + ":" + roomId;
    }

    // 메시지 수신 시 호출. 싱글 스레드 redis increment는 원자적이므로 동시 호출에 안전.
    public void incrementUnread(Long userId, Long roomId) {
        String key = key(userId, roomId);
        // value +1. key가 없으면 0으로 간주
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    // 읽음 처리 시 호출. 키 자체를 삭제한다.
    // value를 0으로 바꾸는 것으로 처리하고, 캐싱 전략을 수정해야할 수 있음.
    public void resetUnread(Long userId, Long roomId) {
        redisTemplate.delete(key(userId, roomId));
    }

    // 방 목록 조회 시 unreadCount 반환
    // 키가 없으면(TTL 만료 또는 아직 미수신) 0을 반환한다. <- 캐싱 전략 수정 시 변경 필요
    public long getUnread(Long userId, Long roomId) {
        String value = redisTemplate.opsForValue().get(key(userId, roomId));
        return value == null ? 0L : Long.parseLong(value);
    }
}
