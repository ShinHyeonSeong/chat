package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class InvalidTokenException extends CustomException{

    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
