package com.calendarbox.backend.schedule.domain;

import com.calendarbox.backend.schedule.enums.RecurrenceFreq;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ScheduleRecurrence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_recurrence_id")
    private Long id;

    @Setter(AccessLevel.PROTECTED)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false, unique = true)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    private RecurrenceFreq freq;

    @Column(name = "interval_count", nullable = false)
    private int intervalCount = 1;

    @Column(name = "by_day", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] byDay;

    @Column(name = "by_monthday", columnDefinition = "integer[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Integer[] byMonthday;

    @Column(name = "by_month", columnDefinition = "integer[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Integer[] byMonth;

    @Column
    private Instant until;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "scheduleRecurrence", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("exceptionDate ASC, id ASC")
    private List<ScheduleRecurrenceException> exceptions = new ArrayList<>();


    private ScheduleRecurrence(RecurrenceFreq freq,
                               int intervalCount,
                               String[] byDay,
                               Integer[] byMonthday,
                               Integer[] byMonth,
                               Instant until){
        this.freq = freq;
        this.intervalCount = intervalCount;
        this.byDay = byDay;
        this.byMonthday = byMonthday;
        this.byMonth = byMonth;
        this.until = until;
    }

    public static ScheduleRecurrence of(
                                        RecurrenceFreq freq,
                                        int intervalCount,
                                        String[] byDay,
                                        Integer[] byMonthday,
                                        Integer[] byMonth,
                                        Instant until) {
        return new ScheduleRecurrence(freq, intervalCount, byDay, byMonthday, byMonth, until);
    }

    public void changeRule(RecurrenceFreq freq, int intervalCount,
                           String[] byDay, Integer[] byMonthday, Integer[] byMonth, Instant until) {
        this.freq = freq;
        this.intervalCount = intervalCount;
        this.byDay = byDay;
        this.byMonthday = byMonthday;
        this.byMonth = byMonth;
        this.until = until;
    }

    public void addException(ScheduleRecurrenceException e) {
        exceptions.add(e);
        e.setScheduleRecurrence(this);
    }
    public void removeException(ScheduleRecurrenceException e) {
        exceptions.remove(e);
        e.setScheduleRecurrence(null);
    }
}
