package com.shin.chat.service;

import com.shin.chat.domain.dto.chat.MessagePageDto;
import com.shin.chat.domain.dto.chat.MessageResponseDto;
import com.shin.chat.domain.entity.ChatRoomEntity;
import com.shin.chat.domain.entity.MessageEntity;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.exception.NotRoomMemberException;
import com.shin.chat.exception.RoomNotFoundException;
import com.shin.chat.repository.ChatRoomMemberRepository;
import com.shin.chat.repository.ChatRoomRepository;
import com.shin.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // 채팅방 메세지 목록 조회 . 커서 페이징 기법 사용
    // cursorId null → 첫 페이지(최신순), non-null → 해당 ID 이전 메시지부터 조회
    public MessagePageDto getMessages(UserEntity me, Long roomId, Long cursorId, int size) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(RoomNotFoundException::new);
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(room, me))
            throw new NotRoomMemberException();

        // size+1 개를 fetch해서 다음 페이지 존재 여부를 판단
        List<MessageEntity> messageEntityList = new LinkedList<>();
        if (cursorId == null) {
            messageEntityList = messageRepository.findByChatRoomOrderByIdDesc(
                    room, PageRequest.of(0, size + 1));
                    // 첫 조회는 가장 최신 메시지
        } else {
            messageEntityList = messageRepository.findByChatRoomAndIdLessThanOrderByIdDesc(
                    room, cursorId, PageRequest.of(0, size + 1));
                    // 커서 기법이므로 오프셋 항상 0
                    // IdLessThan으로 cursorId보다 작은 페이지만 size + 1 개 만큼 만환
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
        List<MessageEntity> page = new LinkedList<>();

        // 현재 페이지 마지막 메시지의 id를 다음 요청의 cursorId로 사용. 마지막 페이지라면 null
        Long nextCursor = null;

        if (hasNext) {
            page = messageEntityList.subList(0, size);
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

    // WebSocket 핸들러(ChatWebSocketController)에서 호출한다.
    // roomId만 알고 있는 상황에서 아래 세 작업을 단일 트랜잭션 안에서 처리한다:
    //   1. 채팅방 조회 + 멤버 검증
    //   2. MessageEntity 저장 (INSERT)
    //   3. ChatRoomEntity.lastMessageId / lastMessageAt 갱신
    //
    // 3번이 같은 트랜잭션 안에 있어야 하는 이유:
    // JPA Dirty Checking은 영속성 컨텍스트(Persistence Context) 안에서 관리되는 엔티티의
    // 필드 변경만 감지한다. 트랜잭션 밖에서 room을 로드하면 Detached 상태가 되어
    // room.updateLastMessage() 를 호출해도 변경이 DB에 반영되지 않는다.
    // 이 메서드 안에서 room을 직접 조회해야 Managed 상태로 유지되며,
    // 트랜잭션 커밋 시 UPDATE 쿼리가 자동 발행된다 (별도 save(room) 호출 불필요).
    @Transactional
    public MessageResponseDto saveMessageToRoom(UserEntity sender, Long roomId, String content) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(RoomNotFoundException::new);
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(room, sender))
            throw new NotRoomMemberException();
        MessageEntity saved = messageRepository.save(new MessageEntity(room, sender, content));
        room.updateLastMessage(saved.getId(), saved.getCreatedAt());
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
