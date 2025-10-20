package com.calendarbox.backend.occurrence.support;

import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public interface RecurrenceExpander {

    record Slice(Instant startUtc, Instant endUtc) {}

    /**
     * 반복 규칙을 from~to 윈도우 안에서 전개하여 occurrence(UTC) 리스트를 돌려줌.
     * - 타임존(zone) 기준으로 전개
     * - 예외(LocalDate)는 스킵
     */
    List<Slice> expand(Schedule s, ScheduleRecurrence r,
                       ZonedDateTime winFrom, ZonedDateTime winTo, ZoneId zone);
}
