package com.calendarbox.backend.friendship.dto.request;

import com.calendarbox.backend.friendship.enums.Action;
import jakarta.validation.constraints.NotNull;

public record FriendRespondRequest(@NotNull Action action) {
}
