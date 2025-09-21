package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;

import java.time.Instant;

public record ScheduleParticipantResponse(
        Long scheduleParticipantId,
        Long scheduleId,
        Long memberId,
        String name,
        Instant invitedAt,
        Instant respondedAt,
        ScheduleParticipantStatus status
) {
}
