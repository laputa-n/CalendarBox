package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.RecurrenceFreq;
import com.calendarbox.backend.schedule.validation.ValidRecurrenceRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@ValidRecurrenceRule
public record RecurrenceUpsertRequest(
        @NotNull RecurrenceFreq freq,
        @NotNull @Min(1) Integer intervalCount,
        Set<String> byDay,
        Set<Integer> byMonthday,
        Set<Integer> byMonth,
        @NotNull Instant until,
        List<LocalDate> exceptions
        ) { }
