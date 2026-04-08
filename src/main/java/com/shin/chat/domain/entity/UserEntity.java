package com.shin.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 ID
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 비밀번호 (암호화 저장)
    @Column(nullable = false)
    private String password;

    // 권한 (USER, ADMIN)
    @Column(length = 20)
    private String role;

    // 계정 상태 (ACTIVE, BANNED 등)
    @Column(length = 20)
    private String status;

    // 생성 시각 (자동 설정)
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 생성자
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
        this.role = "USER";
        this.status = "ACTIVE";
    }

    // 비밀번호 변경 (추후 사용)
    public void changePassword(String password) {
        this.password = password;
    }

    // 상태 변경 (관리자 기능 대비)
    public void changeStatus(String status) {
        this.status = status;
    }
}