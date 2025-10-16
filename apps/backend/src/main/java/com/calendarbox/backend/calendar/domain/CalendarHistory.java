package com.calendarbox.backend.calendar.domain;

import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class CalendarHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Member actor;

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    private CalendarHistoryType type;

    @Column(name = "changed_fields", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> changedFields = Map.of();

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @Builder
    private CalendarHistory(Calendar calendar,
                            Member actor,
                            Long entityId,
                            CalendarHistoryType type,
                            Map<String,Object> changedFields) {
        this.calendar = calendar;
        this.actor = actor;
        this.entityId = entityId;
        this.type = type;
        this.changedFields = (changedFields == null ? Map.of() : changedFields);
    }
}
