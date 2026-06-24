package com.shin.chat.domain.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

// STOMP /pub/chat.send 페이로드 DTO.
// REST API에서는 방 ID를 @PathVariable로 받지만,
// WebSocket에는 URL 경로 변수 개념이 없으므로 roomId를 바디에 포함한다.

// @NoArgsConstructor: Jackson이 JSON → 객체 역직렬화 시 기본 생성자를 사용한다.
// 기본 생성자가 없으면 역직렬화에 실패하므로 반드시 필요하다.
@Getter
@NoArgsConstructor
public class SendMessageRequestDto {
    private Long roomId;
    private String content;
}
