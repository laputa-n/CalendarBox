package com.calendarbox.backend.friendship.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FriendRequest(
        @NotNull @Positive Long addresseeId
) {
}
