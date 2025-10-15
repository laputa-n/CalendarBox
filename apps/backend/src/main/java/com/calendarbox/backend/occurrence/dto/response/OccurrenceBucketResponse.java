package com.calendarbox.backend.occurrence.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record OccurrenceBucketResponse(
        Long calendarId,  // 전체 조회면 null
        Instant fromUtc,
        Instant toUtc,
        Map<LocalDate, List<OccurrenceItem>> days
) {}