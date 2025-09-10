package com.calendarbox.backend.schedule.domain;

import com.calendarbox.backend.place.domain.Place;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class SchedulePlace {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "name")
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    @Builder
    private SchedulePlace(Schedule schedule, Place place, String name) {
        this.schedule = schedule;
        this.place = place;
        this.name = name;
    }

    @Builder
    private SchedulePlace(Schedule schedule, String name) {
        this.schedule = schedule;
        this.name = name;
    }
}
