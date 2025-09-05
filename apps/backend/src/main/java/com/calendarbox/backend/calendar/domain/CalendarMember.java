package com.calendarbox.backend.calendar.domain;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.member.domain.Member;
import jakarta.persistence.*;
import jakarta.validation.groups.Default;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "calendar_member",
        indexes = {
                @Index(name = "idx_cm_calendar", columnList = "calendar_id"),
                @Index(name = "idx_cm_member", columnList = "member_id"),
                @Index(name = "idx_cm_member_status", columnList = "member_id, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cm_calendar_member", columnNames = {"calendar_id", "member_id"})
        }
)
public class CalendarMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarMemberStatus status;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    private CalendarMember(Calendar calendar, Member member, CalendarMemberStatus status, boolean isDefault) {
        this.calendar = Objects.requireNonNull(calendar);
        this.member = Objects.requireNonNull(member);
        this.status = Objects.requireNonNull(status);
        this.isDefault = isDefault;
    }

    public static CalendarMember create(Calendar calendar, Member member, boolean isDefault){
        return new CalendarMember(calendar,member,CalendarMemberStatus.ACCEPTED, isDefault);
    }
    public static CalendarMember invite(Calendar calendar, Member member) {
        return new CalendarMember(calendar, member, CalendarMemberStatus.INVITED, false);
    }

    public void accept() {
        if (this.status == CalendarMemberStatus.ACCEPTED) return;
        this.status = CalendarMemberStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void reject() {
        if (this.status == CalendarMemberStatus.REJECTED) return;
        this.status = CalendarMemberStatus.REJECTED;
        this.respondedAt = Instant.now();
    }

    public void makeDefault() { this.isDefault = true; }
    public void unsetDefault() { this.isDefault = false; }
}
