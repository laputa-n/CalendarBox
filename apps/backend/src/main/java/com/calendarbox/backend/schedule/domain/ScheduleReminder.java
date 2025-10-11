package com.calendarbox.backend.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleReminder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_reminder_id")
    private Long id;

    @Setter(AccessLevel.PROTECTED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "minutes_before", nullable = false)
    private int minutesBefore;

    private ScheduleReminder(int minutesBefore){
        this.minutesBefore = minutesBefore;
    }

    public static ScheduleReminder of(int minutesBefore){
        return new ScheduleReminder(minutesBefore);
    }

    public static ScheduleReminder create(Schedule schedule, int minutesBefore) {
        ScheduleReminder r = new ScheduleReminder();
        r.schedule = schedule;
        r.minutesBefore = minutesBefore;
        return r;
    }
}
