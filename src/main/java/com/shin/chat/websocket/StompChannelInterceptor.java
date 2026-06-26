package com.shin.chat.websocket;

import com.shin.chat.jwt.CustomUserDetails;
import com.shin.chat.jwt.CustomUserDetailsService;
import com.shin.chat.jwt.JwtManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

// WebSocket 인증 인터셉터
// REST API에서는 JwtAuthenticationFilter가 모든 HTTP 요청마다 JWT를 검증한다.
// WebSocket은 최초 연결 이후 HTTP 요청이 발생하지 않으므로 JwtAuthenticationFilter가 동작하지 않는다.
// 대신 STOMP CONNECT 프레임에서 JWT를 꺼내 인증을 수행한다.
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtManager jwtManager;
    private final CustomUserDetailsService customUserDetailsService;

    // preSend는 메시지가 채널로 전달되기 전에 항상 호출된다.
    // CONNECT, SEND, SUBSCRIBE, DISCONNECT 등 모든 STOMP 프레임이 이 메서드를 통과한다.
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // StompHeaderAccessor. STOMP 프레임의 커맨드(CONNECT, SEND 등)와 헤더를 읽는 유틸리티 클래스
        // 생성자를 사용할 경우 원본 내용이 변경되지 않아 MessageHeaderAccessor를 통해 setUser()로 사용자 인증 정보를 저장한다.
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 프레임에서만 인증을 수행. CONNECT 이후 프레임은 사용자 정보가 등록되어있음.
        // accessor가 null인 경우는 SockJs가 동작한 경우. STOMP 이외의 통신수단이므로 그냥 통과시킨다.
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        // STOMP 헤더에서 Authorization 값 읽기
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WebSocket 인증 헤더가 없습니다.");
        }

        // 접두사 제거
        String token = authHeader.substring(7);

        // 서명 검증, 만료 여부 확인
        jwtManager.validateToken(token);

        // 토큰 사용자 조회
        String username = jwtManager.getUsername(token);
        CustomUserDetails userDetails =
                (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);

        // Spring Security 인증 토큰 생성
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // 사용자 인증 정보 등록. 이후 principal 파라미터로 사용 가능.
        accessor.setUser(auth);

        return message;
    }
}
