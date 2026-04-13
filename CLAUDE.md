# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드 문서다.

## 원칙

- 중요도가 높거나 그에 준하는 사항(미구현 기능, 알려진 결함, 임시 처리, 보안 취약점 등)은 **현재 상태** 섹션에 명시한다.
- 매 요청 종료 시 현재 상태 섹션을 검토하고, 완료된 항목은 제거하거나 갱신한다.

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

## 명령어

```bash
./gradlew build       # 프로젝트 빌드
./gradlew bootRun     # 애플리케이션 실행 (포트 8003)
./gradlew test        # 전체 테스트 실행
./gradlew clean       # 빌드 산출물 정리
./gradlew bootWar     # 배포용 WAR 빌드
```

단일 테스트: `./gradlew test --tests "com.shin.chat.SomeTestClass"`

## 아키텍처

사용자 인증 및 채팅 기능을 제공하는 Spring Boot 4.0 REST API. 루트 패키지: `com.shin.chat`.

**레이어 구조:**
- `controller/` → `service/` → `repository/` → MariaDB (`chat` DB, 포트 3306)
- `domain/entity/` — JPA 엔티티
- `domain/dto/` — 요청/응답 DTO; `domain/mapper/` — MapStruct를 이용한 엔티티↔DTO 변환
- `exception/` — `ErrorCode` enum을 가진 `CustomException` 기반 클래스; `GlobalExceptionHandler` (@RestControllerAdvice)가 `ErrorResponseDto` 반환
- `jwt/` — `JwtManager` (HS256, 만료 1시간), `JwtAuthenticationFilter` (Authorization 헤더에서 Bearer 토큰 추출), `SecurityConfig`, `CustomUserDetails`/`CustomUserDetailsService`

**인프라 의존성** (`application.yml`에 설정되어 있으나 아직 미구현):
- Redis (`localhost:6379`) — 캐싱
- Apache Kafka — 대용량 메시지 처리
- WebSocket — 실시간 채팅

## 참조

### 항상 참조 (매 세션 시 로드)
- [컨텍스트 흐름 (요청/인증/예외 처리)](docs/context.md)
- [Git 규칙 (커밋/브랜치/PR)](docs/git.md)

### 필요 시 참조 (직접 언급 시)
- [Exception 패키지 구조 및 추가 방법](src/main/java/com/shin/chat/exception/exception.md)
- [JWT 패키지 구조 및 필터 동작 원리](src/main/java/com/shin/chat/jwt/jwt.md)
- [DB 모델링](docs/db.md)
- [API 테스트 요청 모음](docs/api-test.md)

## 현재 상태

- `UserController` 로그인 엔드포인트 주석 처리됨
- `application.yml`에 DB 비밀번호·JWT 시크릿 하드코딩됨 (**보안 취약점** — 추후 `application-local.yml`로 분리 예정)
- `UserDto`는 비어 있음; 실제 인증에 사용되는 DTO는 `LoginRequestDto` (username/password), `LoginResponseDto` (token)
- `ServletInitializer`를 통해 외부 서블릿 컨테이너 배포용 WAR 패키징 설정됨
- DB 스키마 변경으로 MariaDB 테이블 재생성 필요 (기존 테이블 DROP 후 `docs/db.md` 스키마 적용)
- `UserEntity`의 `role`/`status`가 코드 테이블 FK로 변경됨 — `ddl-auto: validate` 통과 여부 미확인
