package com.calendarbox.backend.schedule.domain;

import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class ScheduleParticipant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ScheduleParticipantStatus status = ScheduleParticipantStatus.INVITED;;

    @CreatedDate
    @Column(name = "invited_at", nullable = false, updatable = false)
    private Instant invitedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    public static ScheduleParticipant ofMember(Schedule schedule, Member member) {
        ScheduleParticipant sp = new ScheduleParticipant();
        sp.schedule = schedule;
        sp.member = member;
        sp.name = member.getName();
        sp.status = ScheduleParticipantStatus.INVITED;
        return sp;
    }

    public static ScheduleParticipant ofName(Schedule schedule, String name) {
        ScheduleParticipant sp = new ScheduleParticipant();
        sp.schedule = schedule;
        sp.name = name;
        sp.status = ScheduleParticipantStatus.ACCEPTED;
        sp.respondedAt = Instant.now();
        return sp;
    }

    public void accept() {
        this.status = ScheduleParticipantStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline() {
        this.status = ScheduleParticipantStatus.REJECTED;
        this.respondedAt = Instant.now();
    }

    void setSchedule(Schedule schedule){
        this.schedule = schedule;
    }

}
