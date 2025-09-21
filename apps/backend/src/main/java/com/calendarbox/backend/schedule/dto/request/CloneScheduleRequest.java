package com.calendarbox.backend.schedule.dto.request;

import java.time.Instant;

public record CloneScheduleRequest(
        Long sourceScheduleId,
        Instant startAt,
        Instant endAt
) {
}
