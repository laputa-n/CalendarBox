package com.calendarbox.backend.analytics.dto.response;

public record DayHourScheduleDistribution(
        Integer dayOfWeek,
        Integer hourOfDay,
        Long scheduleCount
) {
}
