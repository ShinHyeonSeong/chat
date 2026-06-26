package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class RefreshTokenExpiredException extends CustomException {

    public RefreshTokenExpiredException() {
        super(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
