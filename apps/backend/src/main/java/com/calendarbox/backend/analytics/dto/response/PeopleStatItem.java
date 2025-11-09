package com.calendarbox.backend.analytics.dto.response;

public record PeopleStatItem(
        Long memberId,
        String name,
        Integer meetCount,
        Long totalDurationMin,
        Double avgDurationMin,
        Long totalAmount,
        Double avgAmount
) {
}
