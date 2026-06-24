package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 채팅방 상세 응답 DTO. 채팅방 입장시,상세 조회시 API에서 공통으로 사용한다.
@Getter
@AllArgsConstructor
public class ChatRoomDetailDto {

    private Long id;
    private String type;    // "ONE" / "GROUP"

    // ONE(1:1) 방은 name이 null이다 — 클라이언트는 members 목록에서 상대방을 식별한다.
    // SummaryDto의 name(상대방 username 계산)과 의도가 다르다는 점에 주의.
    private String name;

    private List<MemberInfo> members;

    // lastMessageContent는 채팅방 목록에서만 필요.
    private Long lastMessageId;
    private LocalDateTime lastMessageAt;

    // 채팅방 멤버 목록을 위한 inner class
    @Getter
    @AllArgsConstructor
    public static class MemberInfo {
        private Long userId;
        private String username;
        private LocalDateTime joinedAt;
    }
}
