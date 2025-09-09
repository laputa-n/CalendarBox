package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarDetail(
        Long calendarId,
        String name,
        CalendarType type,
        Visibility visibility,
        Integer memberCount,
        Instant createdAt,
        Instant updatedAt
) {
}
