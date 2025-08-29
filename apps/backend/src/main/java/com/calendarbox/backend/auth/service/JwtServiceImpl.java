package com.calendarbox.backend.auth.service;

import com.calendarbox.backend.auth.dto.RefreshClaims;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.auth.dto.SignupClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
    private final JwtClock clock = new JwtClock();

    @Value("${auth.jwt.issuer}")
    private String issuer;

    @Value("${auth.jwt.access.secret}")
    private String accessSecret;              // Base64 또는 충분히 긴 랜덤 문자열
    @Value("${auth.jwt.access.ttl-minutes}")
    private long accessTtlMinutes;

    @Value("${auth.jwt.signup.secret}")
    private String signupSecret;
    @Value("${auth.jwt.signup.ttl-minutes}")
    private long signupTtlMinutes;

    @Value("${auth.jwt.refresh.secret}")
    private String refreshSecret;
    @Value("${auth.jwt.refresh.ttl-minutes}")
    private long refreshTtlMinutes;

    // ===== 임시(회원가입 전용) 토큰 =====
    @Override
    public String issueTempSignupToken(Long kakaoId, String email) {
        Instant now = clock.now();
        SecretKey key = toKey(signupSecret);
        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(signupTtlMinutes, ChronoUnit.MINUTES)))
                .subject(String.valueOf(kakaoId))
                .claim("typ", "signup")
                .claim("email", email)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public SignupClaims verifyTempSignupToken(String token) {
        SecretKey key = toKey(signupSecret);
        JwtParser parser = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build();
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            // typ=signup 강제
            if (!"signup".equals(claims.get("typ", String.class))) {
                throw new IllegalArgumentException("invalid token type");
            }
            Long kakaoId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            return new SignupClaims(kakaoId, email);
        } catch (JwtException | IllegalArgumentException e) {
            // 만료/서명불일치/형식오류 등
            throw new IllegalArgumentException("invalid or expired token");
        }
    }

    // ===== 액세스 토큰 발급(로그인용) =====
    @Override
    public String issueAccessToken(Member member) {
        Instant now = clock.now();
        SecretKey key = toKey(accessSecret);
        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .subject(String.valueOf(member.getId()))
                .claim("typ", "access")
                .claim("name", member.getName())
                .claim("email", member.getEmail())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // ===== helpers =====
    private SecretKey toKey(String secret) {
        // Base64면 decode, 아니면 raw bytes. (32바이트 이상 권장)
        try {
            byte[] bytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(bytes);
        } catch (IllegalArgumentException ignored) {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(bytes);
        }
    }

    // 분리해두면 테스트가 쉬워짐
    static class JwtClock {
        Instant now() { return Instant.now(); }
    }

    @Override
    public Long verifyAccessToken(String token) {
        try {
            SecretKey key = toKey(accessSecret);  // String → SecretKey 변환
            Claims claims = Jwts.parser()        // 0.12.x 에서는 parser() 사용!
                    .verifyWith(key)             // 0.12.x API: verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // subject에 memberId 저장했으니 꺼내서 Long으로 변환
            return Long.valueOf(claims.getSubject());
        } catch (io.jsonwebtoken.JwtException e) {
            throw new io.jsonwebtoken.JwtException("Invalid access token", e);
        }
    }

    //리프레시 발급
    @Override
    public String issueRefreshToken(Member member) {
        Instant now = clock.now();
        SecretKey key = toKey(refreshSecret);
        String jti = java.util.UUID.randomUUID().toString(); // 선택: 회전/추적용

        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtlMinutes, ChronoUnit.MINUTES)))
                .subject(String.valueOf(member.getId()))  // memberId
                .id(jti)                                  // 선택: jti
                .claim("typ", "refresh")
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // 리프레시 검증
    @Override
    public RefreshClaims verifyRefreshToken(String token) {
        SecretKey key = toKey(refreshSecret);
        Claims claims = Jwts.parser()
                .verifyWith(key)    // JJWT 0.12.x
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("invalid token type");
        }
        Long memberId = Long.valueOf(claims.getSubject());
        String jti = claims.getId();
        return new RefreshClaims(memberId, jti);
    }

}
