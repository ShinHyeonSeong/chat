package com.shin.chat.repository;

import com.shin.chat.domain.entity.ChatRoomEntity;
import com.shin.chat.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    Optional<ChatRoomEntity> findByUserAAndUserB(UserEntity userA, UserEntity userB);
}
