package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class InvalidPasswordException extends CustomException {
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }
}
