package com.shin.chat.service;

import com.shin.chat.domain.dto.chat.MessagePageDto;
import com.shin.chat.domain.dto.chat.MessageResponseDto;
import com.shin.chat.domain.entity.ChatRoomEntity;
import com.shin.chat.domain.entity.MessageEntity;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.event.MessageSavedEvent;
import com.shin.chat.exception.NotRoomMemberException;
import com.shin.chat.exception.RoomNotFoundException;
import com.shin.chat.repository.ChatRoomMemberRepository;
import com.shin.chat.repository.ChatRoomRepository;
import com.shin.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final ApplicationEventPublisher eventPublisher;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // 채팅방 메세지 목록 조회. 커서 페이징 기법 사용
    // cursorId null → 첫 페이지(최신순), non-null → 해당 ID 이전 메시지부터 조회
    public MessagePageDto getMessages(UserEntity me, Long roomId, Long cursorId, int size) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(RoomNotFoundException::new);
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(room, me))
            throw new NotRoomMemberException();

        // size+1 개를 fetch해서 다음 페이지 존재 여부를 판단
        List<MessageEntity> messageEntityList = new ArrayList<>();
        if (cursorId == null) {
            messageEntityList = messageRepository.findByChatRoomOrderByIdDesc(
                    room, PageRequest.of(0, size + 1));
                    // 첫 조회는 가장 최신 메시지
        } else {
            messageEntityList = messageRepository.findByChatRoomAndIdLessThanOrderByIdDesc(
                    room, cursorId, PageRequest.of(0, size + 1));
                    // 커서 기법이므로 오프셋 항상 0
                    // IdLessThan으로 cursorId보다 작은 페이지만 size + 1 개 만큼 반환
                    /*
                        SELECT * FROM message
                        WHERE chat_room_id = chatroomId
                        AND id < cursorId
                        ORDER BY id DESC
                        LIMIT size + 1;
                    */
        }

        // 조회 후 size +1개가 있으면 페이지 1개 버림. size 미만일 경우 마지막 페이지로 간주
        boolean hasNext = messageEntityList.size() > size;

        // 현재 페이지 마지막 메시지의 id를 다음 요청의 cursorId로 사용. 마지막 페이지라면 null
        Long nextCursor = null;

        List<MessageEntity> page = new ArrayList<>();
        if (hasNext) {
            page = messageEntityList.subList(0, size); // 기존 30개 message만 사용
            nextCursor = page.get(page.size() - 1).getId();
        } else {
            page = messageEntityList;
        }

        return new MessagePageDto(page.stream().map(this::toDto).toList(), nextCursor);
    }

    // REST 흐름에서 이미 로드된 ChatRoomEntity를 받아 저장할 때 사용한다.
    // 멤버 검증 없이 단순 저장만 수행하므로, 호출 전 검증이 완료된 상황에서만 사용해야 한다.
    @Transactional
    public MessageEntity saveMessage(ChatRoomEntity room, UserEntity sender, String content) {
        return messageRepository.save(new MessageEntity(room, sender, content));
    }


    // 메세지 저장. ChatRoom의 lastMessage는 같은 트랜잭션 내부에서 Dirty Checking으로 함께 update 한다.
    @Transactional
    public MessageResponseDto saveMessageToRoom(UserEntity sender, Long roomId, String content) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(RoomNotFoundException::new);
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(room, sender))
            throw new NotRoomMemberException();
        // 메세지 저장
        MessageEntity saved = messageRepository.save(new MessageEntity(room, sender, content));

        // 해당 채팅방의 마지막 메세지 갱신
        room.updateLastMessage(saved.getId(), saved.getCreatedAt());

        // 안읽음 수를 올릴 대상(발신자 제외)을 트랜잭션 안에서 미리 계산한다.
        // member.getUser().getId()는 식별자만 접근하므로 LAZY 프록시를 초기화하지 않아 N+1이 없다.
        List<Long> recipientIds = chatRoomMemberRepository.findByChatRoom(room).stream()
                .map(member -> member.getUser().getId())
                .filter(userId -> !userId.equals(sender.getId()))
                .toList();

        // 실제 Redis 반영은 트랜잭션 커밋 이후(MessageSavedEventListener)로 미룬다.
        // 저장이 롤백되면 이벤트도 발행되지 않으므로 Redis 카운터 불일치를 막는다.
        eventPublisher.publishEvent(new MessageSavedEvent(roomId, recipientIds));

        return toDto(saved);
    }

    // deletedAt != null이면 메세지 내용 고정 포맷.
    private MessageResponseDto toDto(MessageEntity m) {
        return new MessageResponseDto(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getUsername(),
                m.isDeleted() ? "[삭제된 메시지]" : m.getContent(),
                m.getCreatedAt(),
                m.isDeleted()
        );
    }
}
