package com.calendarbox.backend.friendship.dto.response;

import java.time.Instant;

public record FriendListItem(
        String friendName,
        Instant respondedAt
) {
}
