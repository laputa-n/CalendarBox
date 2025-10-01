package com.calendarbox.backend.friendship.dto.response;

import com.calendarbox.backend.friendship.enums.FriendshipStatus;

import java.time.Instant;

public record ReceivedItemResponse(
        Long friendshipId,
        Long requesterId,
        Long addresseeId,
        String requesterName,
        FriendshipStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
