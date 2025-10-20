package com.calendarbox.backend.occurrence.dto.response;

import java.time.Instant;

public record OccurrenceItem(
        String occurrenceId,
        Long scheduleId,
        Long calendarId,
        String title,
        String theme,
        Instant startAtUtc,
        Instant endAtUtc,
        boolean recurring
) {}