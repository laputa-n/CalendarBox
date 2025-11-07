package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record PlaceMonthlySummary(
        LocalDateTime month,
        Long placeId,
        String placeName,
        Long visitCount,
        Double totalDurationTime
) {
}
