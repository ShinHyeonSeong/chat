package com.shin.chat.domain.dto.chat;

import lombok.Getter;

// PATCH /api/rooms/{id}/read 요청 바디.
@Getter
public class ReadReceiptRequestDto {

    // "모두 읽음" 대신 특정 메시지 ID까지 읽음으로 표시한다.
    // 클라이언트가 화면에 보이는 마지막 메시지 ID를 보내면, 서버는 그 ID까지만 읽음 처리한다.
    // 이렇게 하면 스크롤 위치에 따른 부분 읽음도 정밀하게 표현할 수 있다.
    private Long lastReadMessageId;
}
