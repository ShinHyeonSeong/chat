package com.shin.chat.jwt;

import com.shin.chat.exception.dto.ErrorCode;
import com.shin.chat.exception.dto.ExceptionResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// JwtAuthenticationFilter보다 앞에 등록되어 JWT 예외를 대신 처리하는 필터.
// @RestControllerAdvice(GlobalExceptionHandler)는 DispatcherServlet 이후 MVC 레이어에서만 동작하므로
// 그 앞단인 Security Filter에서 발생한 JWT 예외는 잡을 수 없다.
// 이 필터가 JwtAuthenticationFilter를 감싸는 구조로, 예외 발생 시 HttpServletResponse에 직접 JSON 응답을 작성한다.
@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {

    // Spring MVC의 HttpMessageConverter를 사용할 수 없으므로 ObjectMapper로 직접 직렬화
    private final tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("jwt 예외 필터 진입");
        try {
            // 다음 필터(JwtAuthenticationFilter)로 진행
            // JwtAuthenticationFilter에서 JWT 파싱 시 예외가 발생하면 여기서 잡힌다
            filterChain.doFilter(request, response);
            log.info("jwt 예외 필터 종료");
        } catch (ExpiredJwtException e) {
            // 토큰 서명은 유효하지만 만료된 경우 → 클라이언트는 /refresh로 재발급 시도 가능
            writeErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            // 서명 불일치, 형식 오류 등 → 재로그인 필요
            writeErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    // HttpServletResponse에 ErrorResponseDto를 JSON으로 직접 작성
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());
        response.getWriter().write(
                objectMapper.writeValueAsString(ExceptionResponseDto.errorResponseDto(errorCode))
        );
    }
}