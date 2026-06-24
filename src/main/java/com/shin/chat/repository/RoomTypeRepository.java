package com.shin.chat.repository;

import com.shin.chat.domain.entity.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, Long> {
    Optional<RoomTypeEntity> findByName(String name);
}
