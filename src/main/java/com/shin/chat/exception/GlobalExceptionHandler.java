package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;
import com.shin.chat.exception.dto.ExceptionResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice   // 전역 예외 처리 컨트롤러
public class GlobalExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExceptionResponseDto> handleCustomException(CustomException e) {

        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ExceptionResponseDto.errorResponseDto(errorCode));
    }

    // 커스텀 예외를 벗어난 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponseDto> handleException(Exception e) {
        log.error("전역 예외처리를 벗어남", e);
        return ResponseEntity
                .internalServerError()
                .body(ExceptionResponseDto.errorResponseDto(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
