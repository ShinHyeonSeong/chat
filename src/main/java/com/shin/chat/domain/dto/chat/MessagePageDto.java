package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MessagePageDto {
    private List<MessageResponseDto> messages;
    private Long nextCursor;
}
