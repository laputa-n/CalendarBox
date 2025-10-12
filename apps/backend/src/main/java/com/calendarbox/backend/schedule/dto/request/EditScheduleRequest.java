package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.ScheduleTheme;

import java.time.Instant;
import java.util.Optional;

public record EditScheduleRequest(
        String title,
        String memo,
        ScheduleTheme theme,
        Instant startAt,
        Instant endAt
) {
}
