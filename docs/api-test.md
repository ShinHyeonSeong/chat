# API 테스트 메모

서버 주소: http://localhost:8003

---

## 1. 회원가입 POST /signup

**정상 요청 (201)**
```
POST http://localhost:8003/signup
Content-Type: application/json

{
  "username": "testuser",
  "password": "1234"
}
```

**예외 - 중복 아이디 (409)**
```
POST
http://localhost:8003/signup
Content-Type: application/json

{
  "username": "testuser",
  "password": "5678"
}
```
예상 응답:
```json
{ "status": 409, "message": "이미 사용 중인 아이디입니다." }
```

---

## 2. 로그인 POST /login

**정상 요청 (200)**
```
POST
http://localhost:8003/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "1234"
}
```
예상 응답:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

**예외 - 존재하지 않는 유저 (404)**
```json
{ "username": "nobody", "password": "1234" }
```
예상 응답:
```json
{ "status": 404, "message": "사용자를 찾을 수 없습니다." }
```

**예외 - 비밀번호 불일치 (401)**
```json
{ "username": "testuser", "password": "wrong" }
```
예상 응답:
```json
{ "status": 401, "message": "비밀번호가 일치하지 않습니다." }
```

---

## 3. 토큰 재발급 POST /refresh

**정상 요청 (200)**
```
POST http://localhost:8003/refresh
Content-Type: application/json

{
  "refreshToken": "로그인 응답의 refreshToken 값"
}
```
예상 응답:
```json
{
  "accessToken": "eyJ... (새 토큰)",
  "refreshToken": "eyJ... (기존 토큰 그대로)"
}
```

**예외 - 만료된 토큰 (401)**
예상 응답:
```json
{ "status": 401, "message": "만료된 토큰입니다." }
```

**예외 - Redis 값과 불일치 (401)**
예상 응답:
```json
{ "status": 401, "message": "잘못된 토큰 형식입니다." }
```

---

## 4. 로그아웃 POST /logout

**정상 요청 (200)**
```
POST http://localhost:8003/logout
Authorization: Bearer 로그인_응답의_accessToken값
```
예상 응답: 200 OK (body 없음)

**예외 - 토큰 없이 요청 (401 또는 403)**
Authorization 헤더 없이 요청 → Spring Security가 차단

**예외 - 만료된 AccessToken (401)**
```
Authorization: Bearer 만료된_토큰
```
예상 응답:
```json
{ "status": 401, "message": "만료된 토큰입니다." }
```

---

## 테스트 순서 (권장)

1. `/signup` → 회원가입
2. `/login` → accessToken, refreshToken 저장
3. `/logout` → 로그아웃 (accessToken 사용)
4. `/refresh` → 로그아웃 후 재발급 시도 → InvalidTokenException 확인
5. `/login` → 다시 로그인
6. 만료 테스트: `application.yml`에서 `access-expiration`을 `5000` (5초)으로 임시 변경 후 `/logout` 요청