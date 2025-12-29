package com.calendarbox.backend.expense.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    public Map<String, Object> request(Map<String, Object> body) {
        try {
            return webClient.post()
                    .uri(invokeUrl)
                    .header("X-OCR-SECRET", secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(b -> Mono.error(new RuntimeException(resp.statusCode() + " / " + b))))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (RuntimeException e) {
            // 호출한 쪽에서 task.markFailed(e.getMessage())로 남길 수 있게
            throw e;
        }
    }
}
