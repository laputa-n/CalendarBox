package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record MonthlyScheduleTrend(
        LocalDateTime month,
        Integer scheduleCount
) {
}
