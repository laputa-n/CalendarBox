package com.calendarbox.backend.analytics.service;

import com.calendarbox.backend.analytics.dto.request.ScheduleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPredictService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<Long, String> predictCategories(List<ScheduleSummary> schedules) {
        String url = "http://localhost:8000/predict";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<ScheduleSummary>> entity = new HttpEntity<>(schedules, headers);

        ResponseEntity<Map<Long, String>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<Long, String>>() {}
        );
        return response.getBody();
    }
}

