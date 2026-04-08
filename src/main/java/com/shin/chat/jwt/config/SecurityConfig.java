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
                        .requestMatchers("/login", "/signup", "/refresh").permitAll()
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
