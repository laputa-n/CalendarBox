package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarMemberItem(
        Long calendarMemberId,
        Long memberId,
        String memberName,
        CalendarMemberStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
