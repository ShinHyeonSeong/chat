package com.shin.chat.repository;

import com.shin.chat.domain.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMemberEntity, ChatRoomMemberId> {
    List<ChatRoomMemberEntity> findByUser(UserEntity user);
    List<ChatRoomMemberEntity> findByChatRoom(ChatRoomEntity chatRoom);
    Optional<ChatRoomMemberEntity> findByChatRoomAndUser(ChatRoomEntity chatRoom, UserEntity user);
    boolean existsByChatRoomAndUser(ChatRoomEntity chatRoom, UserEntity user);
}
