package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.enums.ScheduleTheme;

import java.time.Instant;

public record ScheduleDto(
        Long scheduleId,
        Long calendarId,
        String title,
        String memo,
        ScheduleTheme theme,
        Instant startAt,
        Instant endAt,
        Long createdBy,
        Long updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
