package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class RoomNotFoundException extends CustomException {
    public RoomNotFoundException() {
        super(ErrorCode.ROOM_NOT_FOUND);
    }
}