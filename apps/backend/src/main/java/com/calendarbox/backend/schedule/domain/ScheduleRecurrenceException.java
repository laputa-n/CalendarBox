package com.calendarbox.backend.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleRecurrenceException {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_recurrence_exception_id")
    private Long id;

    @Setter(AccessLevel.PROTECTED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_recurrence_id", nullable = false)
    private ScheduleRecurrence scheduleRecurrence;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    private ScheduleRecurrenceException(LocalDate exceptionDate) {
        this.exceptionDate = exceptionDate;
    }
    public static ScheduleRecurrenceException of(LocalDate exceptionDate) {
        return new ScheduleRecurrenceException(exceptionDate);
    }
}
