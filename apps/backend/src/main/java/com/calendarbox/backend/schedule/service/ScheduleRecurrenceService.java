package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrenceException;
import com.calendarbox.backend.schedule.dto.request.RecurrenceUpsertRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceResponse;
import com.calendarbox.backend.schedule.enums.RecurrenceFreq;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleRecurrenceService {
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    public RecurrenceResponse create(Long userId, Long scheduleId, RecurrenceUpsertRequest req) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!schedule.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if(scheduleRecurrenceRepository.existsBySchedule_Id(schedule.getId())) throw new BusinessException(ErrorCode.RECURRENCE_ALREADY_EXISTS);
        if (!req.until().isAfter(schedule.getEndAt()))
            throw new BusinessException(ErrorCode.RECURRENCE_UNTIL_BEFORE_END);

        Set<String> byDay = req.byDay();
        if (byDay != null) {
            byDay = byDay.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toUpperCase())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
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

        var recur = ScheduleRecurrence.of(req.freq(), req.intervalCount(),
                byDayArr, byMonthdayArr, byMonthArr, req.until()
        );

        if (req.exceptions() != null && !req.exceptions().isEmpty()) {
            var uniq = req.exceptions().stream().distinct().sorted().toList();
            for (var exDate : uniq) {
                recur.addException(ScheduleRecurrenceException.of(exDate));
            }
        }
        schedule.makeRecurrence(recur);
        scheduleRepository.flush();
        return toResponse(recur);
    }

    public RecurrenceResponse update(Long userId, Long scheduleId, Long recurrenceId, RecurrenceUpsertRequest req) {
        var r = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        Schedule schedule = r.getSchedule();
        if (!schedule.getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_RECUR_EXDATE_MISMATCH);

        if(!schedule.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if (!req.until().isAfter(schedule.getEndAt()))
            throw new BusinessException(ErrorCode.RECURRENCE_UNTIL_BEFORE_END);

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
        Schedule schedule = r.getSchedule();
        if(!schedule.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if (!schedule.getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_RECUR_EXDATE_MISMATCH);
        schedule.removeRecurrence(r);
    }

    private RecurrenceResponse toResponse(ScheduleRecurrence r) {
        List<LocalDate> exDates = new ArrayList<>();
        for(var ex:r.getExceptions()){
            exDates.add(ex.getExceptionDate());
        }
        return new RecurrenceResponse(
                r.getId(), r.getFreq(), r.getIntervalCount(),
                r.getByDay()==null? List.of(): List.of(r.getByDay()),
                r.getByMonthday()==null? List.of(): List.of(r.getByMonthday()),
                r.getByMonth()==null? List.of(): List.of(r.getByMonth()),
                r.getUntil(), exDates, r.getCreatedAt()
        );
    }
}


