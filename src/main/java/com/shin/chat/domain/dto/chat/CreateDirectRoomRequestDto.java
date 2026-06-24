package com.shin.chat.domain.dto.chat;

import lombok.Getter;

// POST /api/rooms/direct 요청 바디.
// 현재 사용자 정보는 JWT에서 추출하므로 DTO에 포함하지 않는다.
@Getter
public class CreateDirectRoomRequestDto {

    private Long targetUserId;
}
