package com.calendarbox.backend.schedule.dto.response;

import java.time.Instant;

public record ScheduleLinkDto(
        Long scheduleLinkId,
        Long scheduleId,
        String url,
        String label,
        Instant createdAt
) {
}
