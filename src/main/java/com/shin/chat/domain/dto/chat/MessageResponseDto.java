package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MessageResponseDto {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String content;
    private LocalDateTime sentAt;
    private boolean deleted;
}
