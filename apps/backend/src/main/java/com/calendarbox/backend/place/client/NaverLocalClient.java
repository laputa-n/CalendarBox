package com.calendarbox.backend.place.client;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.place.dto.response.NaverLocalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class NaverLocalClient {
    private final WebClient web;

    public NaverLocalClient(
            @Value("${naver.search.client-id}") String id,
            @Value("${naver.search.client-secret}") String secret
    ) {
        this.web = WebClient.builder()
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("X-Naver-Client-Id", id)
                .defaultHeader("X-Naver-Client-Secret", secret)
                .build();
    }

    public List<NaverLocalResponse.Item> search(String query, int display, String sort) {
        return web.get()
                .uri(uri -> uri.path("/v1/search/local.json")
                        .queryParam("query", query)
                        .queryParam("display", Math.min(Math.max(display,1),5))
                        .queryParam("start", 1)
                        .queryParam("sort", sort)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, rsp ->
                        rsp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(
                                        new BusinessException(
                                                ErrorCode.EXTERNAL_API_ERROR,
                                                "Naver local error " + rsp.statusCode().value() + " body=" + body
                                        )
                                ))
                )
                .bodyToMono(NaverLocalResponse.class)
                .map(NaverLocalResponse::items)
                .defaultIfEmpty(List.of())
                .block();
    }
}