package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class PythonEmbeddingService {
    private final WebClient webClient;
    private final String embeddingUrl;

    public PythonEmbeddingService(
            WebClient.Builder builder,
            @Value("${embedding.python.url}") String embeddingUrl
    ) {
        this.webClient = builder.build();
        this.embeddingUrl = embeddingUrl;
    }

    public float[] embed(String text){
        EmbedRequest request = new EmbedRequest(List.of(text));

        EmbedResponse response = webClient.post()
                .uri(embeddingUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbedResponse.class)
                .block();

        if (response == null || response.vectors() == null || response.vectors().isEmpty()) {
            throw new BusinessException(ErrorCode.EMBEDDING_FAIL);
        }

        List<Double> vector = response.vectors().get(0);
        float[] result = new float[vector.size()];
        for(int i = 0; i < vector.size(); i++){
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }

    private record EmbedRequest(List<String> texts) {}
    private record EmbedResponse(List<List<Double>> vectors) {}
}
