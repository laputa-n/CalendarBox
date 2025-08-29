package com.calendarbox.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redis;

    @Value("${auth.jwt.refresh.ttl-minutes}")
    private long refreshTtlMinutes;

    private String key(Long memberId) {
        return "refresh:" + memberId;
    }

    private String hash(String token){
        return DigestUtils.sha256Hex(token);
    }

    /** 로그인/가입 완료 시 저장 */
    public void save(Long memberId, String refreshToken) {
        redis.opsForValue().set(
                key(memberId),
                hash(refreshToken),
                Duration.ofMinutes(refreshTtlMinutes)
        );
    }

    /** 현재 저장된 해시 조회 (필요시) */
    public Optional<String> getHash(Long memberId) {
        return Optional.ofNullable(redis.opsForValue().get(key(memberId)));
    }

    /** 제출된 토큰과 저장된 해시 비교 */
    public boolean matches(Long memberId, String refreshToken){
        String storedHash = redis.opsForValue().get(key(memberId));
        return storedHash != null && hash(refreshToken).equals(storedHash);
    }

    /** 회전(rotate): 새 토큰 해시로 교체 + TTL 갱신 */
    public void replace(Long memberId, String newRefreshToken) {
        save(memberId, newRefreshToken);
    }

    /** 로그아웃/무효화 */
    public void delete(Long memberId) {
        redis.delete(key(memberId));
    }
}
