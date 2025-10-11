package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.enums.ScheduleTheme;

import java.time.Instant;

public record CreateScheduleResponse(
        Long calendarId,
        Long scheduleId,
        String title,
        String memo,
        ScheduleTheme theme,
        Instant startAt,
        Instant endAt,
        Long createdBy,
        Instant createdAt,

        int linkCount,
        int todoCount,
        int reminderCount,
        int participantCount,
        int placeCount,
        Boolean hasRecurrence
        ) {
}
