package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.dto.response.CalendarHistoryDto;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarHistoryQueryService {

    private final CalendarMemberRepository calendarMemberRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final ScheduleRepository scheduleRepository;

    public Page<CalendarHistoryDto> getHistories(
            Long userId,
            Long calendarId,
            Pageable pageable
    ) {
        if (!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(
                calendarId, userId, CalendarMemberStatus.ACCEPTED
        )) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        Page<CalendarHistory> page = calendarHistoryRepository.findPage(calendarId, pageable);

        List<Long> scheduleIds = page.getContent().stream()
                .filter(h -> h.getType() == CalendarHistoryType.SCHEDULE_CREATED
                        || h.getType() == CalendarHistoryType.SCHEDULE_UPDATED
                        || h.getType() == CalendarHistoryType.SCHEDULE_DELETED)
                .map(CalendarHistory::getEntityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        final var scheduleMap = scheduleIds.isEmpty()
                ? java.util.Map.<Long, Schedule>of()
                : scheduleRepository.findAllById(scheduleIds).stream()
                .collect(java.util.stream.Collectors.toMap(Schedule::getId, s -> s));

        return page.map(h -> {
            CalendarHistoryType t = h.getType();
            String actorName = (h.getActor() == null ? null : h.getActor().getName());

            // 1) Schedule 계열
            if (t == CalendarHistoryType.SCHEDULE_CREATED
                    || t == CalendarHistoryType.SCHEDULE_UPDATED
                    || t == CalendarHistoryType.SCHEDULE_DELETED) {

                Schedule s = (h.getEntityId() == null) ? null : scheduleMap.get(h.getEntityId());

                String title = (s != null) ? s.getTitle() : asString(h.getChangedFields().get("title"));
                Instant startAt = (s != null) ? s.getStartAt() : asInstant(h.getChangedFields().get("startAt"));
                Instant endAt = (s != null) ? s.getEndAt() : asInstant(h.getChangedFields().get("endAt"));

                return new CalendarHistoryDto(
                        h.getId(),
                        calendarId,
                        actorName,
                        null,
                        title,
                        startAt,
                        endAt,
                        t,
                        h.getCreatedAt()
                );
            }

            // 2) Member 계열
            if (t == CalendarHistoryType.CALENDAR_MEMBER_ADDED
                    || t == CalendarHistoryType.CALENDAR_MEMBER_REMOVED) {

                // 저장 키가 통일되어 있지 않아서 values 중 첫 값(대개 이름)을 targetName으로 사용
                String targetName = null;
                if (h.getChangedFields() != null && !h.getChangedFields().isEmpty()) {
                    Object first = h.getChangedFields().values().iterator().next();
                    targetName = asString(first);
                }

                return new CalendarHistoryDto(
                        h.getId(),
                        calendarId,
                        actorName,         // 여기 null로 두고 싶으면 null로 바꿔도 됨
                        targetName,
                        null,
                        null,
                        null,
                        t,
                        h.getCreatedAt()
                );
            }

            // 3) Calendar 업데이트 등
            return new CalendarHistoryDto(
                    h.getId(),
                    calendarId,
                    actorName,
                    null,
                    null,
                    null,
                    null,
                    t,
                    h.getCreatedAt()
            );
        });
    }

    private String asString(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return s.isBlank() ? null : s;
    }

    private Instant asInstant(Object v) {
        if (v == null) return null;
        if (v instanceof Instant i) return i;

        // JSON 숫자(BigDecimal/Double/Long 등)
        if (v instanceof Number n) {
            return fromEpoch(new java.math.BigDecimal(n.toString()));
        }

        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;

        // 문자열인데 숫자인 경우도 처리
        if (s.matches("^-?\\d+(\\.\\d+)?$")) {
            return fromEpoch(new java.math.BigDecimal(s));
        }

        // ISO-8601 문자열이면 그대로
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Instant fromEpoch(java.math.BigDecimal bd) {
        // 초/밀리초 판별 (밀리초면 보통 1e12 이상)
        java.math.BigDecimal abs = bd.abs();
        boolean looksLikeMillis = abs.compareTo(new java.math.BigDecimal("100000000000")) >= 0; // 1e11 기준

        if (looksLikeMillis) {
            return Instant.ofEpochMilli(bd.longValue());
        }

        long sec = bd.longValue();
        java.math.BigDecimal frac = bd.subtract(java.math.BigDecimal.valueOf(sec)).abs();
        int nanos = frac.movePointRight(9).intValue(); // 0~999,999,999
        return Instant.ofEpochSecond(sec, nanos);
    }
}
