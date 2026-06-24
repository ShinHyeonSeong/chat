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

// [WebSocket 메시지 핸들러]
// REST 컨트롤러(@RestController)가 HTTP 요청을 처리하듯,
// 이 클래스는 클라이언트가 STOMP SEND 프레임으로 보낸 메시지를 처리한다.
// @RestController는 반환값을 HTTP 응답 바디로     직렬화하지만,
// 여기서는 @Controller만 사용한다. 메서드 반환값 대신 SimpMessagingTemplate으로
// 원하는 구독 대상에게 직접 메시지를 밀어넣는 방식(Push)을 사용하기 때문이다.
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;

    // SimpMessagingTemplate: 서버에서 클라이언트 쪽으로 메시지를 능동적으로 전송할 때 사용한다.
    // REST에서는 요청이 오면 응답을 보내는 요청-응답 모델이지만,
    // WebSocket에서는 서버가 원하는 시점에 구독 중인 모든 클라이언트에게 메시지를 브로드캐스트할 수 있다.
    // @EnableWebSocketMessageBroker가 활성화되면 이 빈이 자동으로 등록된다.
    private final SimpMessagingTemplate messagingTemplate;

    // [메시지 전송 핸들러]
    // @MessageMapping("/chat.send"): WebSocketConfig에서 설정한 /pub prefix와 합쳐져
    // 실제 수신 목적지는 /pub/chat.send 가 된다.
    // 클라이언트가 STOMP SEND 프레임을 /pub/chat.send 목적지로 보내면 이 메서드가 호출된다.
    //
    // 파라미터 주입 방식:
    // - SendMessageRequestDto request: STOMP 프레임의 페이로드(body)를 Jackson이 역직렬화해서 주입한다.
    //   REST의 @RequestBody와 동일한 역할이다.
    // - Principal principal: StompChannelInterceptor에서 accessor.setUser()로 등록한
    //   UsernamePasswordAuthenticationToken이 자동으로 주입된다.
    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequestDto request, Principal principal) {
        UserEntity sender = extractUser(principal);

        // 멤버 검증 + DB 저장 + 채팅방 lastMessage 갱신을 단일 트랜잭션 안에서 처리한다.
        // 반환값은 방금 저장된 메시지를 클라이언트 응답 형식으로 변환한 DTO다.
        MessageResponseDto response = messageService.saveMessageToRoom(
                sender, request.getRoomId(), request.getContent());

        // convertAndSend(destination, payload):
        // /sub/room/{roomId} 를 SUBSCRIBE 하고 있는 모든 클라이언트에게 response를 JSON으로 직렬화해 전송한다.
        // 발신자 포함 같은 방의 모든 구독자가 수신한다.
        // 발신자를 제외하려면 convertAndSendToUser()를 사용해 개별 전송해야 한다.
        messagingTemplate.convertAndSend("/sub/room/" + request.getRoomId(), response);
    }

    // [인증 체인 흐름]
    // 1. 클라이언트가 STOMP CONNECT 프레임을 보낼 때 Authorization 헤더에 Bearer 토큰을 담는다.
    // 2. StompChannelInterceptor.preSend()가 토큰을 검증하고
    //    accessor.setUser(UsernamePasswordAuthenticationToken) 으로 사용자를 세션에 등록한다.
    // 3. 이후 @MessageMapping 메서드의 Principal 파라미터에는 그 토큰이 자동 주입된다.
    //    따라서 Principal의 실제 타입은 UsernamePasswordAuthenticationToken 이다.
    // 4. 토큰의 getPrincipal()은 CustomUserDetails 이고, getUser()로 UserEntity를 꺼낼 수 있다.
    private UserEntity extractUser(Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
