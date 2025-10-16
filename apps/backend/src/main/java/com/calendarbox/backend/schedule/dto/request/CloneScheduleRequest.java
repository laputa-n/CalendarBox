package com.calendarbox.backend.schedule.dto.request;

import java.time.Instant;
import java.time.LocalDate;

public record CloneScheduleRequest(
        Long sourceScheduleId,
        LocalDate targetDate
) {
}
