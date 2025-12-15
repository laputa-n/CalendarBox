package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.schedule.dto.response.GeminiResponse;
import com.calendarbox.backend.place.dto.request.PlaceRecommendRequest;
import com.calendarbox.backend.schedule.enums.ScheduleCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiScheduleCategoryExtractor {

    private final WebClient webClient;
    private final String apiKey;

    private static final String MODEL = "gemini-2.5-flash";

    public GeminiScheduleCategoryExtractor(
            WebClient.Builder builder,
            @Value("${gemini.api-key}") String apiKey
    ) {
        this.webClient = builder
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
        this.apiKey = apiKey;
    }

    public ScheduleCategory extract(PlaceRecommendRequest request) {

        String prompt = buildPrompt(request);

        try {
            GeminiResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + MODEL + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(Map.of(
                            "contents", List.of(
                                    Map.of("parts", List.of(
                                            Map.of("text", prompt)
                                    ))
                            )
                    ))
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            log.info("Gemini response {}", response);
            String text = response.getText().trim().toUpperCase();
            return ScheduleCategory.valueOf(text);
        } catch (Exception e) {
            return ScheduleCategory.OTHER;
        }
    }

    private String buildPrompt(PlaceRecommendRequest req) {
        return """
            Title: %s
            Memo: %s
            Start: %s
            End: %s
            ParticipantCount: %s
            이 스케줄의 카테고리는 무엇일까?
        """.formatted(
                req.title()==null?"":req.title(),
                req.memo()==null?"":req.memo(),
                req.startAt(),
                req.endAt(),
                req.participantCount()
        );
    }

}
