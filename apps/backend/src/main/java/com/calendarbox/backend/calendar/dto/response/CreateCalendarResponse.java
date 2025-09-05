package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;

import java.time.Instant;

public record CreateCalendarResponse(
        Long calendarId, Long ownerId, String name, CalendarType type, Visibility visibility, Instant createdAt
) {
}
