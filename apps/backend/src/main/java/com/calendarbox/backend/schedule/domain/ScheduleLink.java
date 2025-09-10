package com.calendarbox.backend.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_link_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(nullable = false)
    private String url;

    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder
    private ScheduleLink(String url, String label){
        this.url = url;
        this.label = label;
    }

    @PrePersist
    void prePersist(){ this.createdAt = Instant.now(); }

    void setSchedule(Schedule schedule){
        this.schedule = schedule;
    }
}
