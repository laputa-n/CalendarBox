package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record PersonMonthlyExpenseSummary(
        LocalDateTime month,
        Long personId,
        String personName,
        Double totalAmount,
        Double avgAmount,
        Integer sharedScheduleCount
) {
}
