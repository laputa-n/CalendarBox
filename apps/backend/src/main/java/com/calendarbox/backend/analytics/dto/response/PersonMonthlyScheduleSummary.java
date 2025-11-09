package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record PersonMonthlyScheduleSummary(
        LocalDateTime month,
        Long personId,
        String personName,
        Integer meetCount,
        Long totalDurationMin
) {
}
