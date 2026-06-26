package com.shin.chat.websocket.config;

import com.shin.chat.websocket.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 위에서 STOMP 메시지 브로커를 동작시키는 설정을 활성화
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;

    // WebSocket 핸드셰이킹 연결 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 전환을 위한 맨 처음 http 연결 엔드포인트 경로.
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*") // 외부 도메인, 포트에서의 접근을 허용. 추후 특정 도메인으로 변경 필요.
                .withSockJS();          // SockJsFallback. WebSocket을 지원하지 않는 환경에서 폴링, SSE 등의 대체 전송을 허용
    }

    // 메시지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 클라이언트 → 서버 방향 목적지 prefix 설정.
        registry.setApplicationDestinationPrefixes("/pub");

        // 서버 → 클라이언트 방향 목적지 prefix (인메모리 심플 브로커 활성화).
        // enableSimpleBroker는 프로세스 내 메모리 브로커로, 서버가 재시작되면 구독 정보가 사라진다.
        registry.enableSimpleBroker("/sub");
    }

    // 인바운드 채널 인터셉터 등록
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
        // 클라이언트에서 서버로 들어오는 모든 STOMP 메시지는 inbound 채널을 통과한다.
        // 여기에 StompChannelInterceptor를 등록하면 CONNECT 프레임이 왔을 때 Interceptor 내부 로직이 먼저 실행된다.
    }
}
