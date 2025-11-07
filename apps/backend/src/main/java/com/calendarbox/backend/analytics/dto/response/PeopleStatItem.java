package com.calendarbox.backend.analytics.dto.response;

public record PeopleStatItem(
        Long memberId,
        String name,
        Integer meetCount,
        Long totalDurationMin,
        Long avgDurationMin,
        Long totalAmount,
        Long avgAmount
) {
}
