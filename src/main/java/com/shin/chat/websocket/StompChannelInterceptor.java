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

// [WebSocket 인증 인터셉터]
// REST API에서는 JwtAuthenticationFilter가 모든 HTTP 요청마다 JWT를 검증한다.
// WebSocket은 최초 연결 이후 HTTP 요청이 발생하지 않으므로 JwtAuthenticationFilter가 동작하지 않는다.
// 대신 STOMP CONNECT 프레임이 WebSocket 연결의 "로그인" 역할을 하며,
// 이 인터셉터가 해당 프레임에서 JWT를 꺼내 인증을 수행한다.
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtManager jwtManager;
    private final CustomUserDetailsService customUserDetailsService;

    // preSend는 메시지가 채널로 전달되기 전에 항상 호출된다.
    // CONNECT, SEND, SUBSCRIBE, DISCONNECT 등 모든 STOMP 프레임이 이 메서드를 통과한다.
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // StompHeaderAccessor: STOMP 프레임의 커맨드(CONNECT, SEND 등)와 헤더를 읽는 유틸리티 클래스.
        // new StompHeaderAccessor.wrap(message)로 새로 만들면 setUser() 변경이 원본 메시지에 반영되지 않아 principal이 null 값이 뜬다.
        // MessageHeaderAccessor.getAccessor()로 꺼내야 기존 메시지 내부의 accessor를 재사용해 반영할 수 있다.
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 프레임에서만 인증을 수행한다.
        // SEND·SUBSCRIBE 같은 이후 프레임은 이미 세션에 사용자 정보가 등록된 상태이므로
        // 매번 토큰을 다시 검증할 필요가 없다.
        // accessor가 null인 경우는 SockJs가 동작한 경우이다. STOMP 이외의 통신수단 WebSocket이므로 통과시킨다.
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        // STOMP 헤더에서 Authorization 값을 읽는다.
        // 클라이언트는 CONNECT 프레임 헤더에 "Authorization: Bearer {token}" 형식으로 전송해야 한다.
        // REST API와 동일한 헤더 규칙을 재사용한다.
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WebSocket 인증 헤더가 없습니다.");
        }

        // "Bearer " 접두사(7자)를 제거하고 순수 토큰 문자열만 추출한다.
        String token = authHeader.substring(7);

        // JwtManager.validateToken(): 서명 검증 + 만료 여부 확인.
        // void 반환이고 실패 시 TokenExpiredException / InvalidTokenException을 던진다.
        // 예외가 발생하면 Spring WebSocket 인프라가 잡아서 클라이언트에게 STOMP ERROR 프레임으로 전달한다.
        jwtManager.validateToken(token);

        // 토큰의 subject(username) 클레임을 꺼낸 뒤 DB에서 사용자를 조회한다.
        String username = jwtManager.getUsername(token);
        CustomUserDetails userDetails =
                (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);

        // Spring Security의 인증 토큰을 생성한다.
        // 두 번째 인자 credentials(비밀번호 또는 토큰)는 인증을 수행할 때 필요한데, 이미 완료되었으니 제거한다.
        // 세 번째 인자(authorities)를 넘기면 "인증 완료" 상태로 표시된다.
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // accessor.setUser()로 이 WebSocket 세션에 인증 정보를 등록한다.
        // 이후 해당 세션에서 오는 모든 STOMP 메시지(@MessageMapping 메서드)에서
        // Principal 파라미터로 이 auth 객체를 자동으로 받을 수 있다.
        accessor.setUser(auth);

        return message;
    }
}
