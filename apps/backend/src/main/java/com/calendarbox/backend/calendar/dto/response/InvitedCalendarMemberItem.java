package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarType;

import java.time.Instant;

public record InvitedCalendarMemberItem(
        Long calendarMemberId,
        Long calendarId,
        String calendarName,
        CalendarType calendarType,
        String inviterName,
        Instant createdAt
) {
}
