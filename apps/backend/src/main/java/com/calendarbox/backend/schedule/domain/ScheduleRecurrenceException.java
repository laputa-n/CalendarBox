package com.calendarbox.backend.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleRecurrenceException {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_recurrence_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_recurrence_id", nullable = false)
    private ScheduleRecurrence recurrence;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    public static ScheduleRecurrenceException of(ScheduleRecurrence recurrence, LocalDate exceptionDate) {
        ScheduleRecurrenceException e = new ScheduleRecurrenceException();
        e.recurrence = recurrence;
        e.exceptionDate = exceptionDate;
        return e;
    }
}
