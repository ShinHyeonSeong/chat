package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// GET /api/rooms 채팅방 목록 응답 DTO.
// 간소화된 필요 정보만을 제공한다.
// @AllArgsConstructor: 서비스가 모든 값을 한 번에 알고 있으므로 부분 생성이 필요 없다. Setter 없이 불변 객체로 유지한다.
@Getter
@AllArgsConstructor
public class ChatRoomSummaryDto {

    private Long id;

    private String type;    // "ONE" 또는 "GROUP"

    private String name;    // 1대1 방이면 상대방 username, 그룹채팅이면 room.getName()

    // 서비스에서 ID로 MessageEntity를 조회한 뒤 content를 채운다 (현재 N+1 — 추후 최적화 대상).
    private String lastMessageContent;

    private LocalDateTime lastMessageAt;

    // Redis에서 조회한 안읽음 수. DB가 아닌 인메모리 캐시 기반이므로 int가 아닌 long을 쓴다 (Redis INCR 반환 타입).
    private long unreadCount;
}
