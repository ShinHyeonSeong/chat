-- ============================================================
-- Chat DB Schema (MariaDB)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS `chat` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `chat`;

-- ── 코드 테이블 ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `user_role` (
  `id`   INT         NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_user_role_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_status` (
  `id`   INT         NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_user_status_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `room_type` (
  `id`   INT         NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_room_type_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ── 초기 코드 데이터 ─────────────────────────────────────────

INSERT INTO `user_role`   (`name`) VALUES ('USER'), ('ADMIN');
INSERT INTO `user_status` (`name`) VALUES ('ACTIVE'), ('BANNED'), ('DORMANT');
INSERT INTO `room_type`   (`name`) VALUES ('ONE'), ('GROUP');

-- ── 메인 테이블 ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)  NOT NULL,
  `password`   VARCHAR(255) NOT NULL,
  `role_id`    INT          NOT NULL DEFAULT 1,
  `status_id`  INT          NOT NULL DEFAULT 1,
  `created_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_users_username` (`username`),
  CONSTRAINT `fk_users_role`   FOREIGN KEY (`role_id`)   REFERENCES `user_role`(`id`),
  CONSTRAINT `fk_users_status` FOREIGN KEY (`status_id`) REFERENCES `user_status`(`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- 1:1 채팅방 중복 방지: user_a_id < user_b_id 로 정렬하여 저장 (앱 레벨 컨벤션)
CREATE TABLE IF NOT EXISTS `chat_room` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `type_id`         INT          NOT NULL DEFAULT 1,
  `name`            VARCHAR(100)     NULL,
  `created_by`      BIGINT       NOT NULL,
  `user_a_id`       BIGINT           NULL,
  `user_b_id`       BIGINT           NULL,
  `last_message_id` BIGINT           NULL,
  `last_message_at` DATETIME(3)      NULL,
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_one_room` (`user_a_id`, `user_b_id`),
  INDEX `idx_chat_room_last_msg` (`last_message_at` DESC),
  CONSTRAINT `fk_room_type`    FOREIGN KEY (`type_id`)    REFERENCES `room_type`(`id`),
  CONSTRAINT `fk_room_creator` FOREIGN KEY (`created_by`) REFERENCES `users`(`id`),
  CONSTRAINT `fk_room_user_a`  FOREIGN KEY (`user_a_id`)  REFERENCES `users`(`id`),
  CONSTRAINT `fk_room_user_b`  FOREIGN KEY (`user_b_id`)  REFERENCES `users`(`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- last_read_message_id: message_read 테이블 대체. unread count = COUNT(*) WHERE id > last_read_message_id
CREATE TABLE IF NOT EXISTS `chat_room_member` (
  `room_id`              BIGINT      NOT NULL,
  `user_id`              BIGINT      NOT NULL,
  `last_read_message_id` BIGINT          NULL,
  `last_read_at`         DATETIME(3)     NULL,
  `joined_at`            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`room_id`, `user_id`),
  INDEX `idx_crm_user` (`user_id`),
  CONSTRAINT `fk_crm_room` FOREIGN KEY (`room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_crm_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)     ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `message` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `room_id`    BIGINT      NOT NULL,
  `sender_id`  BIGINT      NOT NULL,
  `content`    TEXT        NOT NULL,
  `deleted_at` DATETIME(3)     NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  INDEX `idx_message_room_time` (`room_id`, `created_at` DESC),
  INDEX `idx_message_room_id`   (`room_id`, `id`),
  INDEX `idx_message_sender`    (`sender_id`),
  CONSTRAINT `fk_msg_room`   FOREIGN KEY (`room_id`)   REFERENCES `chat_room`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `users`(`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- ============================================================
-- 설계 문서
-- ============================================================

-- ── 전체 모델링 개요 ─────────────────────────────────────────
--
-- 테이블 분류
--   코드 테이블 (3): user_role, user_status, room_type
--     - 도메인에서 사용하는 고정 코드값을 별도 테이블로 관리한다.
--     - ENUM 대신 코드 테이블을 선택한 이유: ENUM은 새 값 추가 시 ALTER TABLE이 필요하지만,
--       코드 테이블은 INSERT 한 줄로 확장 가능하다.
--     - 각 코드 테이블의 name 컬럼에 UNIQUE INDEX를 두어 중복 코드값을 방지한다.
--
--   메인 테이블 (4): users, chat_room, chat_room_member, message
--     - 실제 서비스 데이터를 저장하는 테이블이다.
--     - 모든 테이블은 InnoDB 엔진을 사용한다 (트랜잭션, FK 지원).
--     - 문자셋은 utf8mb4 / utf8mb4_unicode_ci 로 통일한다.
--       (utf8mb4_0900_ai_ci는 MySQL 전용 콜레이션으로 MariaDB에서 호환성 문제가 발생할 수 있다.)
--     - 타임스탬프는 DATETIME(3) 을 사용한다.
--       (DATETIME은 초 단위까지만 저장되어 같은 초 내에 여러 이벤트가 발생하면 순서 보장 불가)


-- ── 테이블별 설계 의도 ───────────────────────────────────────

-- [users]
--   - username: 로그인 ID. VARCHAR(50) + UNIQUE INDEX로 중복 가입 방지 및 조회 최적화.
--   - password: BCrypt 해시 결과를 저장하므로 VARCHAR(255) 필요.
--   - role_id, status_id: 코드 테이블 FK. NOT NULL + DEFAULT 1로 생성 시 자동으로 USER/ACTIVE 설정.
--     String 대신 FK를 사용함으로써 유효하지 않은 코드값 삽입을 DB 레벨에서 차단한다.

-- [chat_room]
--   - type_id: room_type FK. ONE(1:1) / GROUP 구분.
--   - name: 그룹 채팅방 이름. 1:1 채팅방은 NULL.
--   - created_by: 방 생성자 FK. 방장 권한 기능 추가 시 활용.
--   - user_a_id, user_b_id: 1:1 채팅방 전용 컬럼.
--       저장 규칙: user_a_id = MIN(userIdA, userIdB), user_b_id = MAX(userIdA, userIdB)
--       이 규칙과 UNIQUE INDEX(user_a_id, user_b_id) 조합으로 동일한 두 사용자 간
--       1:1 채팅방 중복 생성을 DB 레벨에서 방지한다.
--       GROUP 타입은 두 컬럼 모두 NULL.
--   - last_message_id, last_message_at: 비정규화 컬럼.
--       채팅방 목록을 최근 메시지 순으로 정렬할 때 message 테이블 전체를 GROUP BY로 집계하면
--       성능 문제가 발생한다. 메시지 저장 시 이 두 컬럼을 함께 갱신하여 O(1) 조회를 가능하게 한다.

-- [chat_room_member]
--   - PK: (room_id, user_id) 복합 기본키. surrogate PK(auto_increment id)는 불필요하다.
--       이미 (room_id, user_id) 조합이 유일하며, 이 쌍으로 항상 조회하기 때문이다.
--   - last_read_message_id: 읽음 처리 핵심 컬럼.
--       기존 message_read 테이블 방식은 메시지 수 × 멤버 수만큼 레코드가 생성되어 확장성 문제가 있다.
--       (예: 메시지 1000건 × 멤버 100명 = 100,000 rows, Kafka 환경에서 중복 이벤트 시 INSERT 충돌)
--       대신 멤버별로 마지막으로 읽은 메시지 ID만 저장하고, 미읽음 수는 다음 쿼리로 계산한다:
--         SELECT COUNT(*) FROM message WHERE room_id = ? AND id > last_read_message_id
--       Kafka consumer에서 읽음 이벤트를 처리할 때 UPDATE는 멱등적으로 동작한다.
--   - last_read_at: 마지막 읽음 시각. "N분 전에 읽음" 같은 UI 표시에 활용.

-- [message]
--   - content TEXT NOT NULL: 현재 텍스트 메시지만 지원. 파일/이미지 타입 추가 시
--       type 컬럼(VARCHAR 또는 코드 테이블 FK)과 content Nullable로 ALTER TABLE하여 확장.
--   - deleted_at: 소프트 삭제. boolean is_deleted 대신 DATETIME을 사용하여 삭제 시각도 보존한다.
--       NULL이면 정상 메시지, 값이 있으면 삭제된 메시지.


-- ── 인덱스 설계 근거 ─────────────────────────────────────────

-- [users]
--   uq_users_username (username)
--     - 로그인 시 username으로 조회하는 쿼리가 가장 빈번하다.
--     - UNIQUE 제약이 인덱스를 겸하므로 별도 인덱스 불필요.

-- [chat_room]
--   uq_one_room (user_a_id, user_b_id)
--     - 1:1 채팅방 생성 시 중복 체크 쿼리: WHERE user_a_id = ? AND user_b_id = ?
--     - UNIQUE 제약이 인덱스를 겸하므로 별도 인덱스 불필요.
--   idx_chat_room_last_msg (last_message_at DESC)
--     - 채팅방 목록을 최근 메시지 순으로 정렬할 때 사용.

-- [chat_room_member]
--   PK (room_id, user_id)
--     - 복합 PK가 두 가지 조회를 모두 커버한다.
--       1) 특정 방의 멤버 목록: WHERE room_id = ? → PK의 앞 컬럼(room_id) prefix 활용
--       2) 특정 멤버인지 확인: WHERE room_id = ? AND user_id = ? → PK 전체 활용
--   idx_crm_user (user_id)
--     - 내 채팅방 목록 조회: WHERE user_id = ?
--     - PK는 room_id가 앞에 오므로 user_id 단독 조회를 커버하지 못한다. 별도 인덱스 필요.

-- [message]
--   idx_message_room_time (room_id, created_at DESC)
--     - 특정 방의 메시지를 시간 역순으로 페이징할 때 사용.
--       WHERE room_id = ? ORDER BY created_at DESC LIMIT N
--   idx_message_room_id (room_id, id)
--     - 미읽음 수 집계 쿼리에 최적화된 인덱스.
--       WHERE room_id = ? AND id > last_read_message_id
--       id는 AUTO_INCREMENT이므로 id 범위 = 시간 범위와 동일하다.
--       PK(id) 단독으로는 room_id 필터를 커버하지 못하므로 복합 인덱스가 필요하다.
--   idx_message_sender (sender_id)
--     - 특정 사용자가 보낸 메시지 조회 시 사용. FK 컬럼은 인덱스가 없으면 full scan 발생.
