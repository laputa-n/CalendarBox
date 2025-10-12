package com.calendarbox.backend.schedule.dto.response;

import java.util.List;

public record ScheduleLinkListResponse(
        int size,
        List<ScheduleLinkDto> scheduleLinkDtos
) {
}
