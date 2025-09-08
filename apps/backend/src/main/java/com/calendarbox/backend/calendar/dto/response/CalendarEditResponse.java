package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.Visibility;

import java.time.Instant;

public record CalendarEditResponse(
        Long calendarId,
        String name,
        Visibility visibility,
        Instant updatedAt
) {
}
