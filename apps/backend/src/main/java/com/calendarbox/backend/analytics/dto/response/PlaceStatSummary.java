package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PlaceStatSummary(
        LocalDateTime month,
        List<PlaceStatItem> top3
) {
}
