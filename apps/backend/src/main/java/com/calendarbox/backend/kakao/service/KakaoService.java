package com.calendarbox.backend.kakao.service;

import com.calendarbox.backend.kakao.dto.KakaoTokenResponseDto;
import com.calendarbox.backend.kakao.dto.KakaoUserInfoResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoService {

    @Value("${kakao.client_id}")
    private String clientId;

    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    // 콘솔에서 Client Secret 사용이 ON인 경우 값 설정(없으면 비어있음)
    @Value("${kakao.client_secret:}")
    private String clientSecret;

    private static final String KAUTH_TOKEN_URL_HOST = "https://kauth.kakao.com";
    private static final String KAPI_USER_URL_HOST   = "https://kapi.kakao.com";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient tokenClient = WebClient.builder()
            .baseUrl(KAUTH_TOKEN_URL_HOST)
            .build();

    private final WebClient userClient  = WebClient.builder()
            .baseUrl(KAPI_USER_URL_HOST)
            .build();

    public String getAccessTokenFromKakao(String code) {
        return exchangeToken(code).getAccessToken();
    }

    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {
        String body = userClient.get()
                .uri("/v2/user/me")
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).map(b -> {
                            log.error("[KAKAO USERINFO] error {} body={}", res.statusCode().value(), b);
                            return new RuntimeException("Kakao userinfo failed");
                        }))
                .bodyToMono(String.class)                 // 먼저 문자열로
                .timeout(Duration.ofSeconds(10))
                .block();

        try {
            return objectMapper.readValue(body, KakaoUserInfoResponseDto.class);
        } catch (Exception parseEx) {
            log.error("[KAKAO USERINFO] parse error. raw body={}", body);
            throw new RuntimeException("Kakao userinfo parse failed");
        }
    }

    public KakaoTokenResponseDto exchangeToken(String code) {
        var form = BodyInserters
                .fromFormData("grant_type", "authorization_code")
                .with("client_id", clientId)
                .with("redirect_uri", redirectUri)
                .with("code", code);
        if (StringUtils.hasText(clientSecret)) {
            form = form.with("client_secret", clientSecret);
        }

        String body = tokenClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        try {
            return objectMapper.readValue(body, KakaoTokenResponseDto.class); // access+refresh
        } catch (Exception e) {
            log.error("[KAKAO TOKEN] parse error. raw body={}", body);
            throw new RuntimeException("Kakao token exchange failed");
        }
    }
}