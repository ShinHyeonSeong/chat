package com.shin.chat.websocket;

import com.shin.chat.domain.dto.chat.MessageResponseDto;
import com.shin.chat.domain.dto.chat.SendMessageRequestDto;
import com.shin.chat.domain.entity.UserEntity;
import com.shin.chat.jwt.CustomUserDetails;
import com.shin.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

// WebSocket 메시지 핸들러. 클라이언트가 STOMP SEND 프레임으로 보낸 메시지를 처리한다.
// 원하는 구독 대상에게 직접 메시지를 밀어넣는 방식(Push)을 사용한다.
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;

    // 서버에서 클라이언트 쪽으로 메시지를 능동적으로 전송할 때 사용한다.
    // @EnableWebSocketMessageBroker가 활성화되면 이 빈이 자동으로 등록된다.
    private final SimpMessagingTemplate messagingTemplate;

    // 메시지 전송 핸들러
    // 클라이언트요청 경로 /pub/chat.send
    @MessageMapping("/chat.send")   // WebSocket으로 SEND된 메세지를 처리할 메서드를 지정하는 어노테이션
    public void sendMessage(SendMessageRequestDto request, Principal principal) {

        // Interceptor에서 인증된 사용자 추출
        UserEntity sender = extractUser(principal);

        // 멤버 검증, 메세지 저장, 마지막 읽은 메세지 갱신, 채팅방의 마지막 메세지 갱신
        MessageResponseDto response = messageService.saveMessageToRoom(
                sender, request.getRoomId(), request.getContent());

        // 해당 토픽의 모든 구독자에게 브로드캐스트
        messagingTemplate.convertAndSend("/sub/room/" + request.getRoomId(), response);
    }

    // 토큰 유저 추출
    private UserEntity extractUser(Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
