package com.calendarbox.backend.notification.domain;

import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Member actor;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson = "{}";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "dedupe_key", unique = true)
    private String dedupeKey;

    public void read(){
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }
}
