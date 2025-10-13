package com.calendarbox.backend.schedule.dto.response;

import java.util.List;

public record ScheduleLinkListResponse(
        int count,
        List<ScheduleLinkDto> scheduleLinkDtos
) {
}
