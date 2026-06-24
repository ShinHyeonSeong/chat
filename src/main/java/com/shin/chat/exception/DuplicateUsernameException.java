package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class DuplicateUsernameException extends CustomException {
    public DuplicateUsernameException() {
        super(ErrorCode.DUPLICATE_USERNAME);
    }
}
