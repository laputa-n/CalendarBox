package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateScheduleRequest(
        @NotNull
        String title,
        String memo,
        ScheduleTheme theme,
        @NotNull
        Instant startAt,
        @NotNull
        Instant endAt
) {
}
