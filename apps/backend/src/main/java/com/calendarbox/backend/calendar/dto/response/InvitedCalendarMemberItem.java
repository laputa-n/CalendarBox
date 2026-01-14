package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;

import java.time.Instant;

public record InvitedCalendarMemberItem(
        Long calendarMemberId,
        Long calendarId,
        String calendarName,
        Visibility calendarVisibility,
        String inviterName,
        Instant createdAt
) {
}
