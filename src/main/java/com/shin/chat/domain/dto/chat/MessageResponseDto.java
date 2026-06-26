package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 단일 메시지 응답 DTO. MessagePageDto의 원소로 사용된다.
@Getter
@AllArgsConstructor
public class MessageResponseDto {

    private Long id;
    private Long senderId;
    private String senderUsername;

    // 소프트 딜리트된 메시지는 서비스에서 "[삭제된 메시지]"로 치환한 뒤 이 필드에 담는다.
    private String content;

    private LocalDateTime sentAt;

    private boolean deleted;
}
