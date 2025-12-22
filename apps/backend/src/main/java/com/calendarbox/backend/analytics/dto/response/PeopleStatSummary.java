package com.calendarbox.backend.analytics.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PeopleStatSummary(
        LocalDateTime month,
        List<PeopleStatItem> top3
) {
}
