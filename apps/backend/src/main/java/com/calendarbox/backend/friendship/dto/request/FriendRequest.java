package com.calendarbox.backend.friendship.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FriendRequest(
        String query
) {
}
