package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;

import java.time.Instant;

public record AddParticipantResponse(
        Long scheduleParticipantId,
        Long scheduleId,
        Long memberId,
        String name,
        ScheduleParticipantStatus status,
        Instant invitedAt
) {
}
