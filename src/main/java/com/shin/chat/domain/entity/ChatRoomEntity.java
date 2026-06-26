package com.shin.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private RoomTypeEntity type;

    @Column(length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id")
    private UserEntity userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id")
    private UserEntity userB;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // create 메서드들 builder 패턴 전환 고려
    public static ChatRoomEntity createDirectRoom(RoomTypeEntity type, UserEntity creator, UserEntity userA, UserEntity userB) {
        ChatRoomEntity room = new ChatRoomEntity();
        room.type = type;
        room.createdBy = creator;
        room.userA = userA;
        room.userB = userB;
        return room;
    }

    public static ChatRoomEntity createGroupRoom(RoomTypeEntity type, UserEntity creator, String name) {
        ChatRoomEntity room = new ChatRoomEntity();
        room.type = type;
        room.createdBy = creator;
        room.name = name;
        return room;
    }

    public void updateLastMessage(Long messageId, LocalDateTime sentAt) {
        this.lastMessageId = messageId;
        this.lastMessageAt = sentAt;
    }
}