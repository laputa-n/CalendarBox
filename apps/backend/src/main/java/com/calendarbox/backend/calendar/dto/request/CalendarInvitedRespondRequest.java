package com.calendarbox.backend.calendar.dto.request;

import com.calendarbox.backend.friendship.enums.Action;
import jakarta.validation.constraints.NotNull;

public record CalendarInvitedRespondRequest(@NotNull Action action) {
}
