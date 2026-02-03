package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarHistoryType;

import java.time.Instant;
import java.util.Map;

public record CalendarHistoryDto(
        Long calendarHistoryId,
        Long calendarId,
        String actorName,
        String targetName,
        String scheduleName,
        Instant scheduleStartAt,
        Instant scheduleEndAt,
        CalendarHistoryType type,
        Instant createdAt
) {
}
