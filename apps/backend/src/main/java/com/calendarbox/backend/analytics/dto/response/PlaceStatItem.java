package com.calendarbox.backend.analytics.dto.response;

public record PlaceStatItem(
        Long placeId,
        String placeName,
        Integer visitCount,
        Long totalStayMin,
        Double avgStayMin,
        Long totalAmount,
        Double avgAmount
) {
}
