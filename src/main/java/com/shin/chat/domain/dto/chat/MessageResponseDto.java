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
    // 원본 content를 그대로 내려보내지 않으므로, 클라이언트는 별도 처리 없이 바로 표시할 수 있다.
    private String content;

    // MessageEntity.createdAt을 매핑한다. 엔티티 필드명과 다르지만 API 의미상 "보낸 시각"이므로 sentAt으로 노출한다.
    private LocalDateTime sentAt;

    // content만으로는 삭제 여부를 알 수 없어 별도 플래그를 둔다. 클라이언트가 취소선·회색 처리 등 UI를 분기할 때 사용한다.
    private boolean deleted;
}
