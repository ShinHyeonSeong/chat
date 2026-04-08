# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 기술 스택

이 프로젝트는 **실시간 채팅 서비스**를 목표로 한다.

| 분류 | 기술                         |
|------|----------------------------|
| 언어 / 프레임워크 | Java 17, Spring Boot 4.0   |
| 인증 | Spring Security, JWT (jjwt 0.11.5) |
| 데이터베이스 | MariaDB (JPA/Hibernate)    |
| 실시간 통신 | WebSocket (STOMP)          |
| 메시지 브로커 | Apache Kafka               |
| 캐싱 | Redis                      |
| ORM 매핑 | JPA                        |
| 빌드 / 배포 | Gradle, WAR (외부 서블릿 컨테이너)  |

## 코드 스타일

| 대상 | 규칙 | 예시 |
|------|------|------|
| 패키지 | 소문자, 단일 단어 | `com.shin.chat.controller` |
| 클래스 / 인터페이스 | PascalCase | `UserService`, `JwtManager` |
| 메서드 | camelCase | `findByUsername()` |
| 변수 | camelCase | `loginRequestDto` |
| 상수 | UPPER_SNAKE_CASE | `TOKEN_HEADER`, `MAX_RETRY_COUNT` |

## Commands

```bash
./gradlew build       # Build the project
./gradlew bootRun     # Run the application (port 8003)
./gradlew test        # Run all tests
./gradlew clean       # Clean build artifacts
./gradlew bootWar     # Build WAR for deployment
```

Single test: `./gradlew test --tests "com.shin.chat.SomeTestClass"`

## Architecture

Spring Boot 4.0 REST API for user authentication and board/chat functionality. Root package: `com.shin.chat`.

**Layer structure:**
- `controller/` → `service/` → `repository/` → MariaDB (`board` DB on port 3306)
- `domain/entity/` — JPA entities
- `domain/dto/` — request/response DTOs; `domain/mapper/` — MapStruct converters between entities and DTOs
- `exception/` — `CustomException` base with `ErrorCode` enum; `GlobalExceptionHandler` (@RestControllerAdvice) returns `ErrorResponseDto`
- `jwt/` — `JwtManager` (HS256, 1hr expiry), `JwtAuthenticationFilter` (extracts Bearer token from Authorization header), `SecurityConfig`, `CustomUserDetails`/`CustomUserDetailsService`

**Infrastructure dependencies** (configured in `application.yml` but not yet fully implemented):
- Redis (`localhost:6379`) — caching
- Apache Kafka — large-scale message processing
- WebSocket — real-time chat

## 참조
- [Exception 패키지 구조 및 추가 방법](src/main/java/com/shin/chat/exception/exception.md)
- [JWT 패키지 구조 및 필터 동작 원리](src/main/java/com/shin/chat/jwt/jwt.md)
- [DB 테이블 생성 쿼리](docs/db.md)
- [API 테스트 요청 모음](docs/api-test.md)

## Current State

- `UserController` login endpoint is commented out
- JWT secret key is hardcoded in `JwtManager`
- `UserDto` is empty; active auth DTOs are `LoginRequestDto` (username/password) and `LoginResponseDto` (token)
- WAR packaging configured via `ServletInitializer` for external servlet container deployment
