package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomSummaryDto {
    private Long id;
    private String name;
    private String type;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
