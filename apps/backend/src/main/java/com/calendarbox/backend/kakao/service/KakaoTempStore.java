package com.calendarbox.backend.kakao.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoTempStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper; // 스프링 빈 사용 권장

    private String key(Long kakaoId) { return "kakao:signup:" + kakaoId; }

    public void save(Long kakaoId, String refreshToken, Map<String,Object> profileJson) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "refreshToken", refreshToken,
                    "profileJson", profileJson
            ));
            redis.opsForValue().set(key(kakaoId), json, Duration.ofMinutes(30)); // TTL은 필요에 맞게
        } catch (Exception e) {
            throw new RuntimeException("temp store serialize failed", e);
        }
    }

    public Optional<TempData> load(Long kakaoId) {
        String json = redis.opsForValue().get(key(kakaoId));
        if (json == null) return Optional.empty();
        try {
            Map<String,Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            String rt = (String) map.get("refreshToken");
            @SuppressWarnings("unchecked")
            Map<String,Object> prof = (Map<String,Object>) map.get("profileJson");
            return Optional.of(new TempData(rt, prof));
        } catch (Exception e) {
            throw new RuntimeException("temp store parse failed", e);
        }
    }

    public void delete(Long kakaoId) { redis.delete(key(kakaoId)); }

    public record TempData(String refreshToken, Map<String,Object> profileJson) {}
}
