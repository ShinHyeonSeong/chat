-- ============================================================
-- init.sql — MariaDB 컨테이너 최초 실행 시 자동 실행되는 초기화 스크립트
-- ============================================================
-- docker-compose.yml의 volumes 설정에 의해
-- /docker-entrypoint-initdb.d/init.sql 경로로 마운트됩니다.
-- MariaDB 이미지는 이 디렉터리의 .sql 파일을 알파벳 순서로 자동 실행합니다.
-- (데이터 볼륨이 비어 있을 때, 즉 최초 실행 시에만 실행됩니다)
-- ============================================================

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- chat 스키마(데이터베이스)가 없으면 생성
-- docker-compose의 MARIADB_DATABASE 환경변수로 이미 생성되지만, 안전하게 중복 선언
CREATE SCHEMA IF NOT EXISTS `chat` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `chat`;

-- 채팅방 테이블
CREATE TABLE IF NOT EXISTS `chat`.`chat_room` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `type`       VARCHAR(20) NULL DEFAULT 'ONE',
  `created_at` DATETIME    NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS `chat`.`users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)  NOT NULL,
  `password`   VARCHAR(255) NOT NULL,
  `role`       VARCHAR(20)  NULL DEFAULT 'USER',
  `status`     VARCHAR(20)  NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username` (`username` ASC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 채팅방 멤버 테이블
CREATE TABLE IF NOT EXISTS `chat`.`chat_room_member` (
  `id`        BIGINT   NOT NULL AUTO_INCREMENT,
  `room_id`   BIGINT   NOT NULL,
  `user_id`   BIGINT   NOT NULL,
  `joined_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `unique_room_user` (`room_id` ASC, `user_id` ASC),
  INDEX `idx_chat_room_member_user` (`user_id` ASC),
  CONSTRAINT `fk_room`
    FOREIGN KEY (`room_id`) REFERENCES `chat`.`chat_room` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user`
    FOREIGN KEY (`user_id`) REFERENCES `chat`.`users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 메시지 테이블
CREATE TABLE IF NOT EXISTS `chat`.`message` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `room_id`    BIGINT   NOT NULL,
  `sender_id`  BIGINT   NOT NULL,
  `content`    TEXT     NOT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `fk_message_sender` (`sender_id` ASC),
  INDEX `idx_message_room_time` (`room_id` ASC, `created_at` DESC),
  CONSTRAINT `fk_message_room`
    FOREIGN KEY (`room_id`)   REFERENCES `chat`.`chat_room` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_message_sender`
    FOREIGN KEY (`sender_id`) REFERENCES `chat`.`users` (`id`)     ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 메시지 읽음 처리 테이블
CREATE TABLE IF NOT EXISTS `chat`.`message_read` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `message_id` BIGINT   NOT NULL,
  `user_id`    BIGINT   NOT NULL,
  `read_at`    DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `unique_message_user` (`message_id` ASC, `user_id` ASC),
  INDEX `idx_message_read_user` (`user_id` ASC),
  CONSTRAINT `fk_read_message`
    FOREIGN KEY (`message_id`) REFERENCES `chat`.`message` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_read_user`
    FOREIGN KEY (`user_id`)    REFERENCES `chat`.`users` (`id`)   ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;