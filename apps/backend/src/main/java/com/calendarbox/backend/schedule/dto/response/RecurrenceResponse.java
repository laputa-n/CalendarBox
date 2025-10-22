package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.enums.RecurrenceFreq;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecurrenceResponse(
        Long recurrenceId, RecurrenceFreq freq, int intervalCount,
        List<String> byDay, List<Integer> byMonthday, List<Integer> byMonth,
        Instant until, List<LocalDate> exDates, Instant createdAt
) {}
