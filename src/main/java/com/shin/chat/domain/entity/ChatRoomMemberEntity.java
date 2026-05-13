package com.shin.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMemberEntity {

    @EmbeddedId
    private ChatRoomMemberId id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoomEntity chatRoom;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    public ChatRoomMemberEntity(ChatRoomEntity chatRoom, UserEntity user) {
        this.id = new ChatRoomMemberId(chatRoom.getId(), user.getId());
        this.chatRoom = chatRoom;
        this.user = user;
    }

    public void updateLastRead(Long messageId, LocalDateTime readAt) {
        this.lastReadMessageId = messageId;
        this.lastReadAt = readAt;
    }
}