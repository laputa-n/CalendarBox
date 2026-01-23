package com.calendarbox.backend.occurrence.support;

import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrenceException;
import com.calendarbox.backend.schedule.enums.RecurrenceFreq;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecurrenceExpanderImpl implements RecurrenceExpander {

    // 안전장치: 한번 호출에서 전개할 최대 occurrence 수
    private static final int MAX_OCCURRENCES = 5000;
    private static final java.util.regex.Pattern BYDAY_TOKEN =
            java.util.regex.Pattern.compile("^([+-]?[1-5])?(MO|TU|WE|TH|FR|SA|SU)$");

    @Override
    public List<Slice> expand(Schedule s, ScheduleRecurrence r,
                              ZonedDateTime winFrom, ZonedDateTime winTo, ZoneId zone) {

        // 시드(원본) 시간대를 zone으로 맞춘다
        ZonedDateTime seedStart = s.getStartAt().atZone(zone);
        ZonedDateTime seedEnd   = s.getEndAt().atZone(zone);

        if (!seedStart.isBefore(seedEnd)) return List.of();

        // 윈도우 상한: until(있으면 min 적용)
        ZonedDateTime upper = winTo;
        Instant untilInstant = r.getUntil(); // 엔티티가 not-null이면 그대로 사용
        if (untilInstant != null) {
            ZonedDateTime untilZ = untilInstant.atZone(zone);
            if (untilZ.isBefore(upper)) upper = untilZ;
        }

        // 예외 날짜 집합(로컬 날짜 기준)
        Set<LocalDate> exceptionDays = Optional.ofNullable(r.getExceptions())
                .orElseGet(List::of)
                .stream()
                .map(ScheduleRecurrenceException::getExceptionDate)
                .collect(Collectors.toCollection(HashSet::new));

        Duration dur = Duration.between(seedStart, seedEnd);

        List<Slice> out = new ArrayList<>(256);

        Set<Instant> seenStarts = new HashSet<>();

        if (seedStart.isBefore(winTo) && seedEnd.isAfter(winFrom)) {
            if (seedStart.isAfter(upper)) {
            } else if (!exceptionDays.contains(seedStart.toLocalDate())) {
                Instant st = seedStart.toInstant();
                if (seenStarts.add(st)) {
                    out.add(new Slice(st, seedEnd.toInstant()));
                }
            }
        }

        RecurrenceFreq freq = r.getFreq();
        int interval = Math.max(1, r.getIntervalCount());

        // WEEKLY에서 사용할 byDay(예: ["MO","WE"])
        Set<DayOfWeek> byDays = normalizeByDays(r.getByDay());

        // 최초 커서를 윈도우 시작에 최대한 가깝게 정렬
        ZonedDateTime cursor = alignToFirst(seedStart, winFrom, freq, interval, byDays);

        while (cursor.isBefore(upper) && out.size() < MAX_OCCURRENCES) {

            // 규칙별 candidate 시작 시각(들)을 계산
            List<ZonedDateTime> candidates = switch (freq) {
                case DAILY -> List.of(cursor);
                case WEEKLY -> projectWeeklyCandidates(cursor, byDays);
                case MONTHLY -> {
                    var ordDays = parseOrdinalDaysStrict(r.getByDay());
                    yield projectMonthlyCandidates(cursor, seedStart, r.getByMonthday(), ordDays);
                }
                case YEARLY -> {
                    var ordDays = parseOrdinalDaysStrict(r.getByDay()); // ordinal 필수 파서
                    var months  = toIntSet(r.getByMonth());             // 1..12 (validator에서 보장)
                    yield projectYearlyCandidates(cursor, seedStart, months, r.getByMonthday(), ordDays, zone);
                }
            };

            for (ZonedDateTime cStart : candidates) {
                ZonedDateTime cEnd = cStart.plus(Duration.between(seedStart, seedEnd));

                // 윈도우 교차 여부
                if (cStart.isBefore(winTo) && cEnd.isAfter(winFrom)) {
                    // 예외(LocalDate)면 스킵
                    if (exceptionDays.contains(cStart.toLocalDate())) continue;

                    Instant st = cStart.toInstant();
                    if (!seenStarts.add(st)) continue;

                    out.add(new Slice(st, cEnd.toInstant()));
                    if (out.size() >= MAX_OCCURRENCES) break;
                }
            }

            // 다음 커서로 이동
            cursor = switch (freq) {
                case DAILY   -> cursor.plusDays(interval);
                case WEEKLY  -> cursor.plusWeeks(interval);
                case MONTHLY -> cursor.plusMonths(interval);
                case YEARLY  -> cursor.plusYears(interval);
            };
        }

        return out;
    }

    /**
     * WEEKLY 전개에서 사용할 요일 후보 생성.
     * byDay가 비어있다면 cursor의 요일 그대로 1개만 반환.
     */
    private List<ZonedDateTime> projectWeeklyCandidates(ZonedDateTime cursor, Set<DayOfWeek> byDays) {
        if (byDays.isEmpty()) return List.of(cursor);
        // cursor가 속한 주의 월요일(ISO)로 정렬
        ZonedDateTime monday = cursor.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<ZonedDateTime> list = new ArrayList<>();
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (byDays.contains(dow)) {
                list.add(monday.with(java.time.temporal.TemporalAdjusters.nextOrSame(dow))
                        .withHour(cursor.getHour())
                        .withMinute(cursor.getMinute())
                        .withSecond(cursor.getSecond())
                        .withNano(cursor.getNano()));
            }
        }
        // 시간순 정렬 후 반환
        list.sort(Comparator.naturalOrder());
        return list;
    }

    /**
     * 최초 커서를 윈도우 시작 근처로 빠르게 보정(성능)
     */
    private ZonedDateTime alignToFirst(ZonedDateTime seedStart, ZonedDateTime winFrom,
                                       RecurrenceFreq freq, int interval, Set<DayOfWeek> byDays) {
        if (!seedStart.isBefore(winFrom)) return seedStart;

        switch (freq) {
            case DAILY -> {
                long days = Duration.between(seedStart, winFrom).toDays();
                long steps = Math.max(0, days / interval);
                return seedStart.plusDays(steps * interval);
            }
            case WEEKLY -> {
                long weeks = Duration.between(seedStart, winFrom).toDays() / 7;
                long steps = Math.max(0, weeks / interval);
                ZonedDateTime base = seedStart.plusWeeks(steps * interval);
                // WEEKLY + byDay: base 주 안에서 가장 이른 후보로 보정
                if (!byDays.isEmpty()) {
                    List<ZonedDateTime> cands = projectWeeklyCandidates(base, byDays);
                    for (ZonedDateTime c : cands) {
                        if (!c.isBefore(winFrom)) return c;
                    }
                    // 모두 창 이전이면 interval 주 뒤의 첫 후보로
                    return projectWeeklyCandidates(base.plusWeeks(interval), byDays).get(0);
                }
                return base;
            }
            case MONTHLY -> {
                // 대략적 보정(정확 월 수 계산은 필요 시 보강)
                Period p = Period.between(seedStart.toLocalDate(), winFrom.toLocalDate());
                int months = p.getYears() * 12 + p.getMonths();
                int steps = Math.max(0, months / interval);
                return seedStart.plusMonths((long) steps * interval);
            }
            case YEARLY -> {
                int years = winFrom.getYear() - seedStart.getYear();
                int steps = Math.max(0, years / interval);
                return seedStart.plusYears((long) steps * interval);
            }
            default -> { return seedStart; }
        }
    }

    private Set<DayOfWeek> normalizeByDays(String[] byDay) {
        if (byDay == null || byDay.length == 0) return Collections.emptySet();
        Set<DayOfWeek> set = new HashSet<>();
        for (String token : byDay) {
            if (token == null || token.isBlank()) continue;
            String t = token.trim().toUpperCase(Locale.ROOT);
            // 서수(예: 3WE, -1SU)는 WEEKLY에서는 무시하고 요일만 취함
            String two = t.substring(Math.max(0, t.length() - 2));
            switch (two) {
                case "MO" -> set.add(DayOfWeek.MONDAY);
                case "TU" -> set.add(DayOfWeek.TUESDAY);
                case "WE" -> set.add(DayOfWeek.WEDNESDAY);
                case "TH" -> set.add(DayOfWeek.THURSDAY);
                case "FR" -> set.add(DayOfWeek.FRIDAY);
                case "SA" -> set.add(DayOfWeek.SATURDAY);
                case "SU" -> set.add(DayOfWeek.SUNDAY);
                default -> {}
            }
        }
        return set;
    }

    private List<OrdinalDay> parseOrdinalDaysStrict(String[] byDay) {
        if (byDay == null || byDay.length == 0) return List.of();
        List<OrdinalDay> out = new java.util.ArrayList<>();
        for (String raw : byDay) {
            var m = BYDAY_TOKEN.matcher(raw.trim().toUpperCase(java.util.Locale.ROOT));
            if (!m.matches() || m.group(1) == null) continue; // 방어적
            int ordinal = Integer.parseInt(m.group(1)); // 1..5 or -1..-5
            DayOfWeek dow = switch (m.group(2)) {
                case "MO" -> DayOfWeek.MONDAY;
                case "TU" -> DayOfWeek.TUESDAY;
                case "WE" -> DayOfWeek.WEDNESDAY;
                case "TH" -> DayOfWeek.THURSDAY;
                case "FR" -> DayOfWeek.FRIDAY;
                case "SA" -> DayOfWeek.SATURDAY;
                case "SU" -> DayOfWeek.SUNDAY;
                default -> throw new IllegalArgumentException("Invalid dow");
            };
            out.add(new OrdinalDay(ordinal, dow));
        }
        return out;
    }

    private java.util.List<ZonedDateTime> projectMonthlyCandidates(
            ZonedDateTime monthAnchor, ZonedDateTime seedStart,
            Integer[] byMonthday, java.util.List<OrdinalDay> ordDays) {

        java.util.List<ZonedDateTime> out = new java.util.ArrayList<>();

        // 1) byMonthday 우선 처리 (예: 15일, 31일)
        if (byMonthday != null && byMonthday.length > 0) {
            for (Integer d : byMonthday) {
                if (d == null) continue;
                out.add(applyDayOfMonthWithNegatives(monthAnchor, d, seedStart));
            }
        }

        // 2) ordinal byDay 처리
        if (ordDays != null && !ordDays.isEmpty()) {
            ZonedDateTime first = monthAnchor.withDayOfMonth(1)
                    .withHour(seedStart.getHour())
                    .withMinute(seedStart.getMinute())
                    .withSecond(seedStart.getSecond())
                    .withNano(seedStart.getNano());
            for (OrdinalDay od : ordDays) {
                ZonedDateTime cand;
                if (od.ordinal > 0) {
                    cand = first.with(java.time.temporal.TemporalAdjusters
                            .dayOfWeekInMonth(od.ordinal, od.dow));
                } else {
                    cand = first.with(java.time.temporal.TemporalAdjusters.lastInMonth(od.dow));
                    int k = Math.abs(od.ordinal);
                    if (k > 1) cand = cand.minusWeeks(k - 1); // -2, -3 등 뒤에서 n번째
                }
                out.add(cand);
            }
        }

        out.sort(java.util.Comparator.naturalOrder());
        return out;
    }

    private record OrdinalDay(Integer ordinal, DayOfWeek dow) {}

    private SortedSet<Integer> toIntSet(Integer[] arr) {
        SortedSet<Integer> set = new TreeSet<>();
        if (arr != null) for (Integer m : arr) if (m != null) set.add(m);
        return set;
    }

    private List<ZonedDateTime> projectYearlyCandidates(
            ZonedDateTime yearAnchor, ZonedDateTime seedStart,
            SortedSet<Integer> months, Integer[] byMonthday, List<OrdinalDay> ordDays, ZoneId zone) {

        List<ZonedDateTime> out = new ArrayList<>();
        int year = yearAnchor.getYear();

        for (int m : months) {
            // 그 해의 해당 월 1일 00:00을 seed 시간대로 입힘
            ZonedDateTime monthAnchor = ZonedDateTime.of(LocalDate.of(year, m, 1), seedStart.toLocalTime(), zone);

            // 1) byMonthday
            if (byMonthday != null && byMonthday.length > 0) {
                for (Integer d : byMonthday) {
                    if (d == null) continue;
                    out.add(applyDayOfMonthWithNegatives(monthAnchor, d, seedStart));
                }
            }

            // 2) ordinal byDay
            if (ordDays != null && !ordDays.isEmpty()) {
                ZonedDateTime first = monthAnchor.withDayOfMonth(1)
                        .withHour(seedStart.getHour())
                        .withMinute(seedStart.getMinute())
                        .withSecond(seedStart.getSecond())
                        .withNano(seedStart.getNano());

                for (OrdinalDay od : ordDays) {
                    ZonedDateTime cand;
                    if (od.ordinal > 0) {
                        cand = first.with(java.time.temporal.TemporalAdjusters
                                .dayOfWeekInMonth(od.ordinal, od.dow));
                    } else { // -1, -2 ...
                        cand = first.with(java.time.temporal.TemporalAdjusters.lastInMonth(od.dow));
                        int k = Math.abs(od.ordinal);
                        if (k > 1) cand = cand.minusWeeks(k - 1);
                    }
                    out.add(cand);
                }
            }
        }

        out.sort(Comparator.naturalOrder());
        return out;
    }

    private ZonedDateTime applyDayOfMonthWithNegatives(
            ZonedDateTime monthAnchor, int daySpec, ZonedDateTime seedStart) {

        LocalDate base = monthAnchor.toLocalDate();
        int len = base.lengthOfMonth();

        int day;
        if (daySpec > 0) {
            day = Math.min(daySpec, len);
        } else { // 음수: 뒤에서 n번째 (-1=말일, -2=말에서 이틀 전 ...)
            int k = Math.abs(daySpec);
            day = Math.max(1, len - k + 1);
        }

        return monthAnchor.withDayOfMonth(day)
                .withHour(seedStart.getHour())
                .withMinute(seedStart.getMinute())
                .withSecond(seedStart.getSecond())
                .withNano(seedStart.getNano());
    }
}
