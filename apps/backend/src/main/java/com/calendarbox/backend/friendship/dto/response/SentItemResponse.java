package com.calendarbox.backend.friendship.dto.response;

import java.time.Instant;

public record SentItemResponse(
        Long friendshipId,
        Long addresseeId,
        String status,
        Instant creadtedAt,
        Instant respondedAt
) { }
