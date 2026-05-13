package com.shin.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp  // insert 쿼리가 발생했을 때, 현재 시간을 자동으로 지정해준다.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public MessageEntity(ChatRoomEntity chatRoom, UserEntity sender, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {                  // 영속성 삭제가 아닌 임시 삭제
        this.deletedAt = LocalDateTime.now();
    }
}