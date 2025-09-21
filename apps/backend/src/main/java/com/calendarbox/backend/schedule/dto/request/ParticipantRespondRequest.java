package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.Action;

public record ParticipantRespondRequest(
        Action action
) {
}
