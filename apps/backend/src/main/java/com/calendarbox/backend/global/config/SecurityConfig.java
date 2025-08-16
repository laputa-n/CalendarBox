package com.calendarbox.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 개발 단계에서는 CSRF 비활성화 (나중에 필요시 켜야 함)
                .csrf(csrf -> csrf.disable())
                // 요청별 접근 정책
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/hello").permitAll()   // 헬스체크용 열어둠
                        .requestMatchers("/members/**").permitAll() // DB 테스트용도 열어둠
                        .anyRequest().authenticated()             // 나머지는 로그인 필요
                )
                // 기본 제공 로그인 페이지 막기
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}