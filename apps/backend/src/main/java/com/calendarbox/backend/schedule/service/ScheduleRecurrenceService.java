package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import com.calendarbox.backend.schedule.dto.request.RecurrenceUpsertRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceResponse;
import com.calendarbox.backend.schedule.enums.RecurrenceFreq;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleRecurrenceService {
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;
    private final ScheduleRepository scheduleRepository;

    // 생성 (스케줄에 여러 개 허용)
    public RecurrenceResponse create(Long userId, Long scheduleId, RecurrenceUpsertRequest req) {
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // until > startAt
        if (!req.until().isAfter(schedule.getStartAt()))
            throw new BusinessException(ErrorCode.RECURRENCE_UNTIL_BEFORE_START);

        // WEEKLY + byDay 비었으면 start 요일 보정 (Seoul 고정)
        Set<String> byDay = req.byDay();
        if (req.freq() == RecurrenceFreq.WEEKLY && (byDay == null || byDay.isEmpty())) {
            var zone = ZoneId.of("Asia/Seoul");
            var dow = schedule.getStartAt().atZone(zone).getDayOfWeek();
            String token = switch (dow) {
                case MONDAY -> "MO"; case TUESDAY -> "TU"; case WEDNESDAY -> "WE";
                case THURSDAY -> "TH"; case FRIDAY -> "FR"; case SATURDAY -> "SA"; case SUNDAY -> "SU";
            };
            byDay = Set.of(token);
        }

        // 중복 제거/정렬
        var byDayArr = (byDay==null? null: byDay.stream().distinct().sorted().toArray(String[]::new));
        var byMonthdayArr = (req.byMonthday()==null? null: req.byMonthday().stream().distinct().sorted().toArray(Integer[]::new));
        var byMonthArr = (req.byMonth()==null? null: req.byMonth().stream().distinct().sorted().toArray(Integer[]::new));

        var saved = scheduleRecurrenceRepository.save(ScheduleRecurrence.of(
                schedule, req.freq(), req.intervalCount(),
                byDayArr, byMonthdayArr, byMonthArr, req.until()
        ));
        return toResponse(saved);
    }

    // 수정 (recurrenceId 기준)
    public RecurrenceResponse update(Long userId, Long scheduleId, Long recurrenceId, RecurrenceUpsertRequest req) {
        var r = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        if (!r.getSchedule().getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_RECUR_EXDATE_MISMATCH);

        // until 검증(스케줄 시작 이후)
        var schedule = r.getSchedule();
        if (!req.until().isAfter(schedule.getStartAt()))
            throw new BusinessException(ErrorCode.RECURRENCE_UNTIL_BEFORE_START);

        // WEEKLY 기본 요일 보정
        Set<String> byDay = req.byDay();
        if (req.freq() == RecurrenceFreq.WEEKLY && (byDay == null || byDay.isEmpty())) {
            var zone = ZoneId.of("Asia/Seoul");
            var dow = schedule.getStartAt().atZone(zone).getDayOfWeek();
            String token = switch (dow) {
                case MONDAY -> "MO"; case TUESDAY -> "TU"; case WEDNESDAY -> "WE";
                case THURSDAY -> "TH"; case FRIDAY -> "FR"; case SATURDAY -> "SA"; case SUNDAY -> "SU";
            };
            byDay = Set.of(token);
        }
        var byDayArr = (byDay==null? null: byDay.stream().distinct().sorted().toArray(String[]::new));
        var byMonthdayArr = (req.byMonthday()==null? null: req.byMonthday().stream().distinct().sorted().toArray(Integer[]::new));
        var byMonthArr = (req.byMonth()==null? null: req.byMonth().stream().distinct().sorted().toArray(Integer[]::new));

        r.changeRule(req.freq(), req.intervalCount(), byDayArr, byMonthdayArr, byMonthArr, req.until());
        return toResponse(r);
    }

    public void delete(Long userId, Long scheduleId, Long recurrenceId) {
        var r = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        if (!r.getSchedule().getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_RECUR_EXDATE_MISMATCH);
        scheduleRecurrenceRepository.delete(r);
    }

    private RecurrenceResponse toResponse(ScheduleRecurrence r) {
        return new RecurrenceResponse(
                r.getId(), r.getFreq(), r.getIntervalCount(),
                r.getByDay()==null? List.of(): List.of(r.getByDay()),
                r.getByMonthday()==null? List.of(): List.of(r.getByMonthday()),
                r.getByMonth()==null? List.of(): List.of(r.getByMonth()),
                r.getUntil(), r.getCreatedAt()
        );
    }
}


