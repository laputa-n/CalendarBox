package com.calendarbox.backend.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleReminder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_reminder_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "minutes_before", nullable = false)
    private int minutesBefore;

    public static ScheduleReminder create(Schedule schedule, int minutesBefore) {
        ScheduleReminder r = new ScheduleReminder();
        r.schedule = schedule;
        r.minutesBefore = minutesBefore;
        return r;
    }
}
