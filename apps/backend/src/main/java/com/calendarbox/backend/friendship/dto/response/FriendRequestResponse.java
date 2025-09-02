package com.calendarbox.backend.friendship.dto.response;

import java.time.Instant;

public record FriendRequestResponse(Long friendshipId, Long requesterId, Long addresseeId, Instant createdAt) {
}
