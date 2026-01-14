package com.calendarbox.backend.schedule.dto.response;

import java.time.Instant;

public record InvitedScheduleParticipantItem(
        Long scheduleParticipantId,
        Long scheduleId,
        String scheduleTitle,
        Instant startAt,
        Instant endAt,
        Long inviterId,
        String inviterName,
        Instant invitedAt
) {
}
