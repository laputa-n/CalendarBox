package com.calendarbox.backend.analytics.dto.request;

public record ScheduleSummary(
        Long scheduleId,
        String title,
        String placeName,
        Double hour,
        Double durationMin,
        Double amount
) {}

