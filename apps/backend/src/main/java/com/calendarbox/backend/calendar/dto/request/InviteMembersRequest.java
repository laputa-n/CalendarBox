package com.calendarbox.backend.calendar.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InviteMembersRequest(
        @NotEmpty(message = "{calendar.invite.members.notempty}")
        List<Long> members
) {
}
