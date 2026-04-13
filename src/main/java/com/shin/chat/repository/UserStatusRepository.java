package com.shin.chat.repository;

import com.shin.chat.domain.entity.UserStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatusEntity, Integer> {
    Optional<UserStatusEntity> findByName(String name);
}
