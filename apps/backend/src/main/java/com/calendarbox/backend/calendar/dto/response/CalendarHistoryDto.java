package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarHistoryType;

import java.time.Instant;
import java.util.Map;

public record CalendarHistoryDto(
        Long calendarHistoryId,
        Long calendarId,
        Long actorId,
        Long entityId,
        CalendarHistoryType type,
        Map<String,Object> changedFields,
        Instant createdAt
) {
}
