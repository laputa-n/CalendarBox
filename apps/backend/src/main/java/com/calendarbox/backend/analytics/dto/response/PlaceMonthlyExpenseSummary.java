package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;

public record PlaceMonthlyExpenseSummary(
        LocalDateTime month,
        Long placeId,
        String placeName,
        Integer visitCount,
        Long totalStayMin,
        Long totalAmount,
        Double avgAmount
) {}
