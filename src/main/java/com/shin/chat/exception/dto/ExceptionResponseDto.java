package com.shin.chat.exception.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExceptionResponseDto {

    private int status;
    private String message;

    public static ExceptionResponseDto errorResponseDto(ErrorCode errorCode) {

        return new ExceptionResponseDto(
                errorCode.getHttpStatus().value(),
                errorCode.getMessage()
        );
    }
}
