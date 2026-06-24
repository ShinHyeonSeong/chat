package com.shin.chat.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// GET /api/rooms/{id}/messages 페이지네이션 응답 DTO.
// 오프셋 방식(LIMIT n OFFSET k) 대신 커서 방식을 채택했다.
// 오프셋은 새 메시지가 삽입될 때 페이지 경계가 밀려 중복·누락이 생기지만,
// 커서는 "이 ID보다 이전 메시지"를 조회하므로 실시간 삽입에도 일관성이 유지된다.
@Getter
@AllArgsConstructor
public class MessagePageDto {

    // 최신순(id DESC)으로 정렬된 메시지 목록. 클라이언트가 역방향으로 렌더링한다.
    private List<MessageResponseDto> messages;

    // 다음 페이지 요청 시 cursorId 파라미터로 사용할 값 — 현재 페이지 마지막 메시지의 id.
    // null이면 더 이상 조회할 메시지가 없음을 의미한다 (마지막 페이지).
    private Long nextCursor;
}
