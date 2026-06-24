package com.shin.chat.websocket.config;

import com.shin.chat.websocket.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// @EnableWebSocketMessageBroker
// WebSocket 위에서 STOMP 메시지 브로커를 동작시키는 설정을 활성화한다.
// STOMP(Simple Text Oriented Messaging Protocol)는 WebSocket 위에서 메시지를 구조화하는
// 서브프로토콜이다. 목적지(destination) 기반 라우팅, 헤더, 구독/발행 패턴을 제공한다.
// 이 어노테이션이 없으면 아래 configureMessageBroker / registerStompEndpoints 설정이 무시된다.
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;

    // [WebSocket 연결 엔드포인트 등록]
    // 클라이언트는 이 경로로 최초 HTTP 요청을 보내고, 서버가 101 Switching Protocols 응답을 보내면
    // 연결이 HTTP에서 WebSocket 프로토콜로 업그레이드된다.
    // 업그레이드 이후부터는 지속 연결이 유지되며, STOMP 프레임 단위로 통신한다.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 전환을 위한 맨 처음 http 연결 엔드포인트 경로.
        registry.addEndpoint("/ws")

                // SockJS Fallback:
                // WebSocket을 지원하지 않는 환경(일부 구형 브라우저, 특정 프록시 서버 등)에서는
                // HTTP Long Polling, Server-Sent Events 등 대체 전송 방식을 자동으로 선택한다.
                // 클라이언트 JS 코드에서 new SockJS('/ws')로 연결하면 이 폴백이 활성화된다.
                .withSockJS();
    }

    // [메시지 브로커 설정]
    // 클라이언트와 서버 간 메시지 흐름의 라우팅 규칙을 정의한다.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 클라이언트 → 서버 방향 목적지 prefix.
        // 클라이언트가 /pub/chat.send 로 메시지를 보내면 Spring이 @MessageMapping("/chat.send")
        // 메서드로 라우팅한다. /pub 이 없으면 라우팅이 되지 않는다.
        registry.setApplicationDestinationPrefixes("/pub");

        // 서버 → 클라이언트 방향 목적지 prefix (인메모리 심플 브로커 활성화).
        // SimpMessagingTemplate.convertAndSend("/sub/room/1", dto) 처럼 서버가 메시지를 보내면,
        // /sub/room/1 을 구독(SUBSCRIBE)하고 있는 모든 클라이언트에게 메시지가 전달된다.
        // enableSimpleBroker는 프로세스 내 메모리 브로커이므로 서버가 재시작되면 구독 정보가 사라진다.
        // 여러 서버 인스턴스 간 브로드캐스트가 필요하면 RabbitMQ·ActiveMQ 같은 외부 브로커로 교체해야 한다.
        registry.enableSimpleBroker("/sub");
    }

    // [인바운드 채널 인터셉터 등록]
    // 클라이언트에서 서버로 들어오는 모든 STOMP 메시지는 inbound 채널을 통과한다.
    // 여기에 StompChannelInterceptor를 등록하면 CONNECT 프레임이 왔을 때
    // JWT 검증 로직이 먼저 실행된다.
    // 인터셉터가 예외를 던지거나 null을 반환하면 메시지 처리가 중단된다.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }
}
