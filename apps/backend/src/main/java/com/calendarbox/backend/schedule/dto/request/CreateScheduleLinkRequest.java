package com.calendarbox.backend.schedule.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateScheduleLinkRequest(
        @NotNull String url,
        String label
) {
}
