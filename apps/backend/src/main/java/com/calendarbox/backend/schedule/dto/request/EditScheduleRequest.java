package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.ScheduleTheme;

import java.time.Instant;
import java.util.Optional;

public record EditScheduleRequest(
        Optional<String> title,
        Optional<String> memo,
        Optional<ScheduleTheme> theme,
        Optional<Instant> startAt,
        Optional<Instant> endAt
) {
}
