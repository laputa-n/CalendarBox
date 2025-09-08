package com.calendarbox.backend.calendar.dto.response;

import java.util.List;

public record InviteMembersResponse(
        Long calendarId,
        int successCount,
        int failureCount,
        List<Long> successIds,
        List<Long> failureIds
) {
}
