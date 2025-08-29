package com.calendarbox.backend.kakao.service;

import com.calendarbox.backend.kakao.dto.KakaoTokenResponseDto;
import com.calendarbox.backend.kakao.dto.KakaoUserInfoResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class KakaoService {
    @Value("${kakao.client_id}")
    private String clientId;
    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    private static final String KAUTH_TOKEN_URL_HOST = "https://kauth.kakao.com";
    private static final String KAPI_USER_URL_HOST   = "https://kapi.kakao.com";

    private final WebClient tokenClient = WebClient.builder().baseUrl(KAUTH_TOKEN_URL_HOST).build();
    private final WebClient userClient  = WebClient.builder().baseUrl(KAPI_USER_URL_HOST).build();

    public String getAccessTokenFromKakao(String code) {
        KakaoTokenResponseDto dto = tokenClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUri)   // ✅ 필수
                        .with("code", code)
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.error(new RuntimeException("Invalid Parameter")))
                .onStatus(HttpStatusCode::is5xxServerError, r -> Mono.error(new RuntimeException("Internal Server Error")))
                .bodyToMono(KakaoTokenResponseDto.class)
                .block();

        return dto.getAccessToken();
    }

    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {
        return userClient.get()
                .uri("/v2/user/me")
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.error(new RuntimeException("Invalid Parameter")))
                .onStatus(HttpStatusCode::is5xxServerError, r -> Mono.error(new RuntimeException("Internal Server Error")))
                .bodyToMono(KakaoUserInfoResponseDto.class)
                .block();
    }
}