package com.calendarbox.backend.friendship.dto.response;

import java.time.Instant;

public record FriendListItem(
        Long memberId,
        String friendName,
        Instant respondedAt
) {
}
