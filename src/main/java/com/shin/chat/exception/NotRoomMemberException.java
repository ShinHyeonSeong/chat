package com.shin.chat.exception;

import com.shin.chat.exception.dto.ErrorCode;

public class NotRoomMemberException extends CustomException {
    public NotRoomMemberException() {
        super(ErrorCode.NOT_ROOM_MEMBER);
    }
}