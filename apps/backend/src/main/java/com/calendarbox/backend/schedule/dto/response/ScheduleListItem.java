package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarType;

import java.time.Instant;

public record ScheduleListItem(
        Long calendarId,
        CalendarType calendarType,
        String calendarName,
        Long scheduleId,
        String scheduleTitle,
        Instant startAt,
        Instant endAt
) {
}
