# 로그인 흐름

## 요청 → 응답 전체 흐름

```
POST /login  { username, password }
    │
    ▼
JwtExceptionFilter              ← 필터 레이어 전체를 try-catch로 감싸 JWT 예외를 JSON으로 변환
    │  try {
    ▼
JwtAuthenticationFilter         ← Authorization 헤더 없음 → 그냥 통과
    │  }
    ▼
SecurityConfig                  ← /login 은 permitAll() → 인증 없이 통과
    │
    ▼
UserController.login()          ← @PostMapping("/login"), LoginRequestDto 바인딩
    │
    ▼
UserService.login()
    │
    ├─ 1. DB 조회
    │      UserRepository.findByUsername(username)
    │      └─ 없으면 → UserNotFoundException (404)
    │
    ├─ 2. 비밀번호 검증
    │      BCryptPasswordEncoder.matches(raw, encoded)
    │      └─ 불일치 → InvalidPasswordException (401)
    │
    ├─ 3. 토큰 발급
    │      JwtManager.createAccessToken(username)   → 만료: 1일
    │      JwtManager.createRefreshToken(username)  → 만료: 5일
    │
    ├─ 4. RefreshToken 저장
    │      RedisTokenService.saveRefreshToken(username, refreshToken)
    │      key="refresh:{username}", TTL=5일
    │
    └─ 5. 응답 반환
           LoginResponseDto { accessToken, refreshToken }
    │
    ▼
HTTP 200 OK
{
  "accessToken":  "eyJ...",
  "refreshToken": "eyJ..."
}
```

---

## 예외 발생 시

| 상황 | 예외 | HTTP |
|------|------|------|
| username 없음 | `UserNotFoundException` | 404 |
| 비밀번호 불일치 | `InvalidPasswordException` | 401 |

예외는 MVC 레이어의 `GlobalExceptionHandler`가 잡아 아래 형식으로 반환한다.

```json
{
  "status": 401,
  "message": "비밀번호가 일치하지 않습니다."
}
```

---

---

## Redis 캐시 저장 (RefreshToken)

Redis는 **주 저장소가 아닌 인메모리 캐시**로만 사용한다. MariaDB에는 refresh token을 저장하지 않는다.

| 항목 | 내용 |
|------|------|
| Key | `refresh:{username}` |
| Value | RefreshToken JWT 문자열 |
| TTL | 5일 |
| 저장 시점 | 로그인 성공 시 |
| 삭제 시점 | 로그아웃 시 |
| 조회 시점 | 토큰 갱신 요청 시 |

**캐시 만료(TTL 소멸) 또는 Redis 재시작 시 동작:**
- 토큰 정보가 소멸되어 갱신 요청이 실패한다 (`TokenExpiredException` 401)
- 이는 버그가 아니라 의도된 동작이다 — 사용자는 재로그인해야 한다

---

## 관련 클래스

| 클래스 | 역할 |
|--------|------|
| `UserController` | 요청 수신, DTO 바인딩 |
| `UserService` | 인증 로직, 토큰 발급 조율 |
| `UserRepository` | JPA, MariaDB `users` 테이블 조회 |
| `JwtManager` | HS256 토큰 생성 |
| `RedisTokenService` | RefreshToken 저장 (Redis) |
| `GlobalExceptionHandler` | 서비스 예외 → JSON 에러 응답 변환 |