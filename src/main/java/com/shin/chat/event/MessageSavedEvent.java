package com.shin.chat.event;

import java.util.List;

// 메시지 저장 도메인 이벤트
// recipientIds: 안읽음 수를 올릴 대상 사용자 ID 목록.
public record MessageSavedEvent(Long roomId, List<Long> recipientIds) {
}
