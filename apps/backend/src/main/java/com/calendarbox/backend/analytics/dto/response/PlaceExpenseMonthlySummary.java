package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record PlaceExpenseMonthlySummary(
        LocalDateTime month,
        Long placeId,
        String placeName,
        Double totalAmount,
        Double avgAmount,
        Integer visitCount,
        Double totalDurationMin
) {}
