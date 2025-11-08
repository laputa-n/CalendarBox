package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record PlaceMonthlyScheduleSummary(
        LocalDateTime month,
        Long placeId,
        String placeName,
        Integer visitCount,
        Long totalStayTime
) {
}
