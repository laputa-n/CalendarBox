package com.calendarbox.backend.place.dto.request;

import java.time.OffsetDateTime;

public record PlaceRecommendRequest(
        String regionQuery,
        String title,
        String memo,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer participantCount
) {
}
