package com.calendarbox.backend.occurrence.support;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public interface OccurrenceSplitter {

    record Slice(Instant startUtc, Instant endUtc) {}

    /**
     * 여러 날짜에 걸친 일정을 사용자 타임존 기준으로 “일 단위”로 잘라줍니다.
     */
    default List<Slice> splitIfMultiDay(Instant startUtc, Instant endUtc, ZoneId zone) {
        List<Slice> out = new ArrayList<>();
        ZonedDateTime s = startUtc.atZone(zone);
        ZonedDateTime e = endUtc.atZone(zone);

        if (!s.isBefore(e)) { // 잘못된 입력 방어
            out.add(new Slice(startUtc, endUtc));
            return out;
        }

        ZonedDateTime curStart = s;
        while (true) {
            ZonedDateTime dayEnd = curStart.toLocalDate().plusDays(1).atStartOfDay(zone);
            ZonedDateTime curEnd = e.isBefore(dayEnd) ? e : dayEnd;
            out.add(new Slice(curStart.toInstant(), curEnd.toInstant()));
            if (!curEnd.isBefore(e)) break;
            curStart = curEnd;
        }
        return out;
    }
}
