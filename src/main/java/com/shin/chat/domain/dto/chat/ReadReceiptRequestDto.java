package com.shin.chat.domain.dto.chat;

import lombok.Getter;

@Getter
public class ReadReceiptRequestDto {
    private Long lastReadMessageId;
}
