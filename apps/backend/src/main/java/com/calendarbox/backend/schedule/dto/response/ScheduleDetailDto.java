package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleTheme;

import java.time.Instant;

public record ScheduleDetailDto(
        Long scheduleId,
        Long calendarId,
        String title,
        String memo,
        ScheduleTheme theme,
        Instant startAt,
        Instant endAt,
        Long createdBy,
        Long updatedBy,
        Instant createdAt,
        Instant updatedAt,
        ScheduleDetailSummary summary
) {
    public static ScheduleDetailDto of(Schedule s, ScheduleDetailSummary summary) {
        return new ScheduleDetailDto(
                s.getId(),
                s.getCalendar().getId(),
                s.getTitle(),
                s.getMemo(),
                s.getTheme(),
                s.getStartAt(),
                s.getEndAt(),
                s.getCreatedBy().getId(),
                s.getUpdatedBy() != null ? s.getUpdatedBy().getId() : null,
                s.getCreatedAt(),
                s.getUpdatedAt(),
                summary
        );
    }
}
