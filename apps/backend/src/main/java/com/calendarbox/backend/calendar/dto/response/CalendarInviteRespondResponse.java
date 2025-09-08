package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;

import java.time.Instant;

public record CalendarInviteRespondResponse(
        Long calendarMemberId,
        Long calendarId,
        Long memberId,
        CalendarMemberStatus status,
        Instant respondedAt
) {
}
