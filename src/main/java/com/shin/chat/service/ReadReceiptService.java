package com.shin.chat.service;

import com.shin.chat.domain.entity.ChatRoomEntity;
import com.shin.chat.domain.entity.ChatRoomMemberEntity;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.exception.NotRoomMemberException;
import com.shin.chat.exception.RoomNotFoundException;
import com.shin.chat.redis.RedisUnreadService;
import com.shin.chat.repository.ChatRoomMemberRepository;
import com.shin.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReadReceiptService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final RedisUnreadService redisUnreadService;

    // @Transactional 범위는 DB만 보호.
    // Redis 삭제(resetUnread)는 트랜잭션 밖 개념이므로, DB 커밋 후 Redis 삭제 실패 시
    // 안읽음 수가 잔존할 수 있다. 허용 가능한 수준의 불일치로 처리한다.
    @Transactional
    public void updateReadReceipt(UserEntity me, Long roomId, Long lastReadMessageId) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        // 방 멤버 여부 검증 + lastReadMessageId / lastReadAt dirty checking으로 DB 반영
        ChatRoomMemberEntity member = chatRoomMemberRepository.findByChatRoomAndUser(room, me)
                .orElseThrow(NotRoomMemberException::new);
        // ChatRoomMember 마지막 읽은 메세지 갱신
        member.updateLastRead(lastReadMessageId, LocalDateTime.now());
        // Redis 안읽음 수 카운터 초기화
        redisUnreadService.resetUnread(me.getId(), roomId);
    }
}
