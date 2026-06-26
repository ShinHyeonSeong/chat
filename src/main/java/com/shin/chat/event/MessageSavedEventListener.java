package com.shin.chat.event;

import com.shin.chat.redis.RedisUnreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 메시지 저장 이벤트 리스너
// 트랜잭션이 정상 커밋된 직후 호출된다. 롤백 시에는 호출되지 않는다.
@Component
@RequiredArgsConstructor
public class MessageSavedEventListener {

    private final RedisUnreadService redisUnreadService;

    // 채팅방 멤버의 안읽음 카운터 수
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSaved(MessageSavedEvent event) {
        Long roomId = event.roomId();
        for (Long userId : event.recipientIds()) {
            redisUnreadService.incrementUnread(userId, roomId);
        }
    }
}
