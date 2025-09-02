package com.calendarbox.backend.friendship.dto.response;

import com.calendarbox.backend.friendship.enums.FriendshipStatus;

import java.time.Instant;

public record FriendRespondResponse(
        Long friendshipId,
        Long requesterId,
        Long addresseeId,
        FriendshipStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
