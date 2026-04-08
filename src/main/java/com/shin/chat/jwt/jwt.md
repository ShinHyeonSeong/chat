# jwt 패키지

JWT 기반 인증 처리를 담당하는 패키지.

---

## 클래스 구성

| 클래스 | 역할 |
|--------|------|
| `JwtManager` | JWT 생성 · 파싱 · 검증 |
| `JwtAuthenticationFilter` | 요청마다 JWT를 검증하고 SecurityContext에 인증 정보 등록 |
| `JwtExceptionFilter` | JWT 관련 예외를 잡아 JSON 에러 응답 작성 |
| `CustomUserDetails` | Spring Security가 사용하는 유저 정보 래퍼 |
| `CustomUserDetailsService` | DB에서 유저를 조회해 `CustomUserDetails` 반환 |
| `config/SecurityConfig` | 필터 체인 · 인가 규칙 · PasswordEncoder 설정 |

---

## 필터 실행 순서

```
요청
 │
 ▼
JwtExceptionFilter          ← JWT 예외 감시 (try-catch 래퍼)
 │   try {
 ▼
JwtAuthenticationFilter     ← JWT 파싱 · 인증 처리
 │   - Bearer 토큰 없음 → 그냥 통과 (Security가 접근 제어)
 │   - 토큰 있음 → getUsername() 호출 (서명·만료 검증 포함)
 │     - 유효 → SecurityContext에 인증 등록 후 통과
 │     - 만료 → ExpiredJwtException 전파 → JwtExceptionFilter catch
 │     - 위조 → JwtException 전파 → JwtExceptionFilter catch
 ▼
UsernamePasswordAuthenticationFilter (Spring Security 기본)
 │
 ▼
DispatcherServlet → Controller
```

---

## 왜 필터가 두 개인가

`@RestControllerAdvice`(`GlobalExceptionHandler`)는 **DispatcherServlet 이후 MVC 레이어**에서만 동작한다.
JWT 필터는 그보다 앞단인 Servlet Filter 레이어에서 실행되므로 예외가 `GlobalExceptionHandler`에 도달하지 못한다.

`JwtExceptionFilter`는 `filterChain.doFilter()`를 `try-catch`로 감싸 이 문제를 해결한다.
`filterChain.doFilter()`는 동기 호출이므로, 이후 필터에서 던진 예외가 호출 스택을 타고 올라와 `catch`에서 잡힌다.

```
JwtExceptionFilter.doFilterInternal()
  try {
    filterChain.doFilter()          // 동기 호출 — 반환 전까지 이후 전체 스택 실행
      JwtAuthenticationFilter.doFilterInternal()
        jwtManager.getUsername()
          → ExpiredJwtException 🔥
        ↑ 전파
      ↑ 전파
  } catch (ExpiredJwtException e)   // 여기서 잡힘
      writeErrorResponse(...)
```

---

## 에러 응답 형식

JWT 예외 발생 시 `ErrorResponseDto` 형식으로 응답한다.

| 예외 | ErrorCode | HTTP Status |
|------|-----------|-------------|
| `ExpiredJwtException` | `EXPIRED_TOKEN` | 401 |
| `JwtException` (기타) | `INVALID_TOKEN` | 401 |

```json
{
  "status": 401,
  "message": "만료된 토큰입니다."
}
```

---

## SecurityConfig 필터 등록 순서

`JwtExceptionFilter`가 `JwtAuthenticationFilter`를 감싸야 하므로 **먼저** 등록한다.

```java
.addFilterBefore(new JwtAuthenticationFilter(...), UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(new JwtExceptionFilter(),         JwtAuthenticationFilter.class)
```

`addFilterBefore(A, B)`는 "A를 B 앞에 등록"한다는 의미이므로
최종 실행 순서는 `JwtExceptionFilter` → `JwtAuthenticationFilter`가 된다.