package com.shin.chat.repository;

import com.shin.chat.domain.entity.ChatRoomEntity;
import com.shin.chat.domain.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByChatRoomOrderByIdDesc(ChatRoomEntity chatRoom, Pageable pageable);
    List<MessageEntity> findByChatRoomAndIdLessThanOrderByIdDesc(ChatRoomEntity chatRoom, Long cursorId, Pageable pageable);
}
