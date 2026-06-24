package com.shin.chat.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtManager jwtManager;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info("jwt 필터 진입");

        // 1. Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = request.getHeader("Authorization");
        log.info("1. {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 토큰 없음 → 인증 없이 다음 필터로 진행 (Security가 접근 제어)
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 2. 이미 인증된 요청은 중복 처리 방지
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            log.info("이미 인증된 사용자. 중복 처리 방지");
            return;
        }

        // 3. 토큰에서 username 추출 (서명, 만료 검증 포함)
        // JwtException, ExpiredJwtException 발생 시 JwtExceptionFilter로 전파
        String username = jwtManager.getUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 5. 인증 객체 생성 및 SecurityContext 등록
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,                          // 인증 완료 후 credentials 불필요
                        userDetails.getAuthorities()
                );
        log.info("인증 객체 생성 완료. {}", userDetails.getUsername());

        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
                // IP 접근 기록 추적, 특정 IP 차단 등의 Security 관련 이벤트에 사용 가능. 미사용시 추후 삭제 필요.
        );

        // SecurityContextHolder에 유저 정보 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.info("jwt 필터 종료.");

        // 6. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
