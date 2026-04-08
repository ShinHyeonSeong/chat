# Exception 패키지

## 구조

```
exception/
├── CustomException.java          # 커스텀 예외 베이스 클래스
├── UserNotFoundException.java    # 사용자 미존재 예외
├── InvalidPasswordException.java # 비밀번호 불일치 예외
├── GlobalExceptionHandler.java   # 전역 예외 처리
└── dto/
    ├── ErrorCode.java            # HTTP 상태코드 + 메시지 정의 (enum)
    └── ErrorResponseDto.java     # 에러 응답 형식
```

## 예외 처리 흐름

```
throw new XxxException()
    → CustomException(ErrorCode.XXX)
    → GlobalExceptionHandler.handleCustomException()
    → ResponseEntity<ErrorResponseDto>
```

커스텀 예외 외의 모든 예외는 `handleException()`이 잡아 500으로 응답한다.

## ErrorCode

| 상수 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. |
| `INVALID_PASSWORD` | 401 | 비밀번호가 일치하지 않습니다. |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

## 응답 형식

```json
{
  "status": 404,
  "message": "사용자를 찾을 수 없습니다."
}
```

## 새 예외 추가 방법

1. `ErrorCode`에 항목 추가
2. `CustomException`을 상속받는 예외 클래스 생성

```java
// 1. ErrorCode 추가
DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),

// 2. 예외 클래스 생성
public class DuplicateUsernameException extends CustomException {
    public DuplicateUsernameException() {
        super(ErrorCode.DUPLICATE_USERNAME);
    }
}
```

`GlobalExceptionHandler`는 수정하지 않아도 된다. `@ExceptionHandler(CustomException.class)`가 하위 클래스를 모두 처리한다.