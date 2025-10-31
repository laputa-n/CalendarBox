package com.calendarbox.backend.expense.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class NaverOcrClient {
    private final WebClient webClient;

    @Value("${naver.ocr.invoke-url}")
    private String invokeUrl;
    @Value("${naver.ocr.secret-key}")
    private String secretKey;

    public NaverOcrClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Map<String, Object> request(String imageUrl) {
        Map<String, Object> body = Map.of(
                "version", "V2",
                "requestId", "req-" + System.currentTimeMillis(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", "jpg",
                        "name", "receipt",
                        "url", imageUrl
                ))
        );

        return webClient.post()
                .uri(invokeUrl)
                .header("X-OCR-SECRET", secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
