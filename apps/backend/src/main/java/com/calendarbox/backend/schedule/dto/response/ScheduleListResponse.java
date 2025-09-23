package com.calendarbox.backend.schedule.dto.response;

import org.springframework.scheduling.annotation.Schedules;

import java.util.List;

public record ScheduleListResponse(
        int count,
        List<ScheduleListItem> scheduleList
) {
}
