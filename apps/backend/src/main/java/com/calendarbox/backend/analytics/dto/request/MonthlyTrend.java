package com.calendarbox.backend.analytics.dto.request;

import java.time.LocalDateTime;

public record MonthlyTrend(
        LocalDateTime month,
        Long count
) {}

