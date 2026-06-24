package com.shin.chat.domain.dto.chat;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateGroupRoomRequestDto {

    // 그룹 채팅방은 이름 필수
    private String name;

    // 초대할 사용자 ID 목록. 빈 리스트도 허용.
    // 생성자 본인은 서비스에서 별도로 멤버에 추가할 것.
    private List<Long> invitedUserIds;
}
