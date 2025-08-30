package com.calendarbox.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebConfig {
    @Value("#{'${security.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration(); // allowCredentials(true) 를 쓰면 * 를 쓸 수 없습니다. 정확한 오리진을 적어주세요.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Signup-Token", "X-Refresh-Token"));  // 프론트가 보낼 헤더들));
        config.setExposedHeaders(List.of("Authorization")); // 응답에서 브라우저가 읽게 할 헤더(필요 시)
        config.setAllowCredentials(true); // 쿠키/인증정보 포함 허용
        config.setMaxAge(3600L); // preflight 캐시(초)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
