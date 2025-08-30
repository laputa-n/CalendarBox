package com.calendarbox.backend.kakao.domain;

import com.calendarbox.backend.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Entity
@Table(name = "kakao_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KakaoAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kakao_account_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "provider_user_id", nullable = false, unique = true)
    private Long providerUserId;

    @Column(name = "connected_at")
    private Instant connectedAt;

    @Column(name = "refresh_token")
    private String refreshToken;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_json", columnDefinition = "jsonb")
    private Map<String, Object> profileJson;

    @Builder
    public KakaoAccount(Member member, Long providerUserId,
                         String refreshToken, Map<String,Object> profileJson) {
        this.member = member;
        this.providerUserId = providerUserId;
        this.connectedAt = Instant.now();
        this.refreshToken = refreshToken;
        this.profileJson = profileJson;
    }
}
