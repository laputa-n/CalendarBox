package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.AddParticipantMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddParticipantRequest(
        @NotNull
        AddParticipantMode mode,
        Long memberId,
        String name
) {
}
