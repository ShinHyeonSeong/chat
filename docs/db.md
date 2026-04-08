-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema chat
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema chat
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `chat` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `chat` ;

-- -----------------------------------------------------
-- Table `chat`.`chat_room`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat`.`chat_room` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`type` VARCHAR(20) NULL DEFAULT 'ONE',
`created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `chat`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat`.`users` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`username` VARCHAR(50) NOT NULL,
`password` VARCHAR(255) NOT NULL,
`role` VARCHAR(20) NULL DEFAULT 'USER',
`status` VARCHAR(20) NULL DEFAULT 'ACTIVE',
`created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `username` (`username` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `chat`.`chat_room_member`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat`.`chat_room_member` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`room_id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`joined_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `unique_room_user` (`room_id` ASC, `user_id` ASC) VISIBLE,
INDEX `idx_chat_room_member_user` (`user_id` ASC) VISIBLE,
CONSTRAINT `fk_room`
FOREIGN KEY (`room_id`)
REFERENCES `chat`.`chat_room` (`id`)
ON DELETE CASCADE,
CONSTRAINT `fk_user`
FOREIGN KEY (`user_id`)
REFERENCES `chat`.`users` (`id`)
ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `chat`.`message`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat`.`message` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`room_id` BIGINT NOT NULL,
`sender_id` BIGINT NOT NULL,
`content` TEXT NOT NULL,
`created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
INDEX `fk_message_sender` (`sender_id` ASC) VISIBLE,
INDEX `idx_message_room_time` (`room_id` ASC, `created_at` DESC) VISIBLE,
CONSTRAINT `fk_message_room`
FOREIGN KEY (`room_id`)
REFERENCES `chat`.`chat_room` (`id`)
ON DELETE CASCADE,
CONSTRAINT `fk_message_sender`
FOREIGN KEY (`sender_id`)
REFERENCES `chat`.`users` (`id`)
ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `chat`.`message_read`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat`.`message_read` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`message_id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`read_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `unique_message_user` (`message_id` ASC, `user_id` ASC) VISIBLE,
INDEX `idx_message_read_user` (`user_id` ASC) VISIBLE,
CONSTRAINT `fk_read_message`
FOREIGN KEY (`message_id`)
REFERENCES `chat`.`message` (`id`)
ON DELETE CASCADE,
CONSTRAINT `fk_read_user`
FOREIGN KEY (`user_id`)
REFERENCES `chat`.`users` (`id`)
ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
