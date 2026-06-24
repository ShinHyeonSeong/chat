package com.shin.chat.jwt.config;


// Security 설정 파일.
// SpringSecurity 5.4 이상에서는 WebSecurityConfigurerAdapter를 사용하지 않음

// @EnableWebSecurity
// public class SecurityConfig extends WebSecurityConfigurerAdapter {}

import com.shin.chat.jwt.CustomUserDetailsService;
import com.shin.chat.jwt.JwtAuthenticationFilter;
import com.shin.chat.jwt.JwtExceptionFilter;
import com.shin.chat.jwt.JwtManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtManager jwtManager;
    private final CustomUserDetailsService customUserDetailsService;

    // service에서 사용할 PasswordEncoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http

                // CSRF 공격 방어 비활성화
                .csrf(csrf -> csrf.disable())

                // 세션 관리 정책 비활성화
                // JWT 토큰을 사용할 것이므로 불필요.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // form 로그인 비활성화
                .formLogin(form -> form.disable())

                // 권한별 인가 설정. 관리자 권한 별도 설정 추후 필요.
                .authorizeHttpRequests(auth -> auth
                        // /ws/** : WebSocket 핸드셰이크 경로를 Spring Security에서 허용한다.
                        // WebSocket 연결은 HTTP Upgrade 요청으로 시작되므로 이 필터를 통과한다.
                        // 허용하지 않으면 JwtAuthenticationFilter가 Bearer 토큰 없음으로 판단해 연결을 차단한다.
                        // 실제 사용자 인증은 StompChannelInterceptor가 CONNECT 프레임에서 별도로 수행하므로
                        // 여기서 열어두더라도 인증되지 않은 사용자가 채팅에 접근하지는 못한다.
                        .requestMatchers("/login", "/signup", "/refresh", "/ws/**").permitAll()
                        .anyRequest().authenticated()   // 기타 경로는 인증된 사용자만 접근
                )

                // JWT 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtManager, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                )
                // JwtExceptionFilter를 JwtAuthenticationFilter 앞에 등록하여 예외 처리 필터를 감싼다.
                .addFilterBefore(
                        new JwtExceptionFilter(),
                        JwtAuthenticationFilter.class
                )

                .build();
    }
}
