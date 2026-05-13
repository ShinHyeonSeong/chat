package com.shin.chat.domain.dto.chat;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateGroupRoomRequestDto {
    private String name;
    private List<Long> invitedUserIds;
}
