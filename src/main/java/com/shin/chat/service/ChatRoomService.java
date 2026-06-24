package com.shin.chat.service;

import com.shin.chat.domain.dto.chat.ChatRoomDetailDto;
import com.shin.chat.domain.dto.chat.ChatRoomSummaryDto;
import com.shin.chat.domain.dto.chat.CreateGroupRoomRequestDto;
import com.shin.chat.domain.entity.*;
import com.shin.chat.exception.*;
import com.shin.chat.exception.dto.ErrorCode;
import com.shin.chat.redis.RedisUnreadService;
import com.shin.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RedisUnreadService redisUnreadService;

    @Transactional
    public ChatRoomDetailDto createDirectRoom(UserEntity me, Long targetUserId) {
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);
        UserEntity userA = null;
        UserEntity userB = null;

        if(me.getId() < targetUserId) {
            userA = me;
            userB = target;
        } else {
            userA = target;
            userB = me;
        }

        Optional<ChatRoomEntity> chatRoomEntity = chatRoomRepository.findByUserAAndUserB(userA, userB);
        if(chatRoomEntity.isPresent()) {
            return toDetailDto(chatRoomEntity.get());
        }

        // Room Type 하드 코딩.
        RoomTypeEntity type = getRoomType("ONE");
        ChatRoomEntity room = ChatRoomEntity.createDirectRoom(type, me, userA, userB);
        chatRoomRepository.save(room);
        chatRoomMemberRepository.save(new ChatRoomMemberEntity(room, userA));
        chatRoomMemberRepository.save(new ChatRoomMemberEntity(room, userB));
        return toDetailDto(room);
    }

    @Transactional
    public ChatRoomDetailDto createGroupRoom(UserEntity me, CreateGroupRoomRequestDto memberList) {
        // Room Type 하드 코딩.
        RoomTypeEntity type = getRoomType("GROUP");

        // 동일 멤버의 중복 그룹 채팅방 허용.
        ChatRoomEntity room = ChatRoomEntity.createGroupRoom(type, me, memberList.getName());
        chatRoomRepository.save(room);

        chatRoomMemberRepository.save(new ChatRoomMemberEntity(room, me));
        for (Long userId : memberList.getInvitedUserIds()) {
            UserEntity invitee = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
            chatRoomMemberRepository.save(new ChatRoomMemberEntity(room, invitee));
        }
        return toDetailDto(room);
    }


    @Transactional
    public ChatRoomDetailDto joinRoom(UserEntity me, Long roomId) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        //그룹 채팅방이 아닌 경우
        if (!room.getType().getName().equals("GROUP")) {
            throw new CustomException(ErrorCode.INVALID_ROOM_TYPE);
        }
        // 이미 존재하는 유저인 경우
        if (chatRoomMemberRepository.existsByChatRoomAndUser(room, me))
            throw new CustomException(ErrorCode.ALREADY_ROOM_MEMBER);

        chatRoomMemberRepository.save(new ChatRoomMemberEntity(room, me));
        return toDetailDto(room);
    }

    // 채팅방 목록 조회
    public List<ChatRoomSummaryDto> getMyRooms(UserEntity me) {
        return chatRoomMemberRepository.findByUser(me).stream()
                .map(chatRoomMember -> toSummaryDto(chatRoomMember, me))
                .toList();
    }

    // 채팅방 상세 조회. 채팅방 멤버가 아닐 시 예외 발생
    public ChatRoomDetailDto getRoomDetail(UserEntity me, Long roomId) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(room, me))
            throw new NotRoomMemberException();
        return toDetailDto(room);
    }

    // ChatRoomEntity를 채팅방 상세정보인 DetailDto로 반환해주는 메서드
    // 채팅방 종류에 따라 메개변수를 다르게 주도록 메서드 분리가 필요할 수도 있음.
    ChatRoomDetailDto toDetailDto(ChatRoomEntity room) {
        // 채팅방 멤버 목록 조회
        List<ChatRoomDetailDto.MemberInfo> members = chatRoomMemberRepository.findByChatRoom(room)
                .stream()
                .map(m -> new ChatRoomDetailDto.MemberInfo(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getJoinedAt()))
                .toList();

        return new ChatRoomDetailDto(
                room.getId(),
                room.getType().getName(),
                room.getName(),
                members,
                room.getLastMessageId(),
                room.getLastMessageAt());
    }

    // 채팅방 요약 정보인 SummaryDto를 반환해주는 메서드.
    // 요약 정보에는 채팅방 마지막 메세지 내용 및 발송시간, 사용자가 안읽은 메세지 수 등이 포함된다.
    private ChatRoomSummaryDto toSummaryDto(ChatRoomMemberEntity chatRoomMember, UserEntity me) {
        ChatRoomEntity room = chatRoomMember.getChatRoom();
        // 채팅방 이름 설정
        String name = resolveName(room, me);

        // 채팅방 마지막 메시지 설정
        String lastContent = resolveLastContent(room);

        // 안읽은 메시지 수 설정
        long unread = redisUnreadService.getUnread(me.getId(), room.getId());

        return new ChatRoomSummaryDto(room.getId(), name, room.getType().getName(),
                lastContent, room.getLastMessageAt(), unread);
    }

    // 채팅방 이름 포맷 과정
    private String resolveName(ChatRoomEntity room, UserEntity me) {
        // 1:1 채팅일 경우, 상대방의 이름으로 설정
        if ("ONE".equals(room.getType().getName())) {
            UserEntity other = room.getUserA().getId().equals(me.getId())
                    ? room.getUserB() : room.getUserA();
            return other.getUsername();
        }
        // 그룹 채팅방일 경우, 채팅방 생성 시점에 정한 이름으로 설정
        return room.getName();
    }

    // 해당 채팅방 마지막 메세지 조회
    private String resolveLastContent(ChatRoomEntity room) {
        // 채팅방에 메세지가 없으면 null
        if (room.getLastMessageId() == null) return null;

        // 채팅방 마지막 메세지 반환. 삭제된 메세지라면 고정 포맷 사용.
        return messageRepository.findById(room.getLastMessageId())
                .map(m -> m.isDeleted() ? "[삭제된 메시지]" : m.getContent())
                .orElse(null);
    }

    // 채팅방 타입 조회
    private RoomTypeEntity getRoomType(String name) {
        return roomTypeRepository.findByName(name)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
