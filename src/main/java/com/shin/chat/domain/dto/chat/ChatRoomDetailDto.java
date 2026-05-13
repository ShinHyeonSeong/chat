package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatRoomDetailDto {
    private Long id;
    private String type;
    private String name;
    private List<MemberInfo> members;
    private Long lastMessageId;
    private LocalDateTime lastMessageAt;

    @Getter
    @AllArgsConstructor
    public static class MemberInfo {
        private Long userId;
        private String username;
        private LocalDateTime joinedAt;
    }
}
