package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class TokenExpiredException extends CustomException{

    public TokenExpiredException() {
        super(ErrorCode.EXPIRED_TOKEN);
    }
}
