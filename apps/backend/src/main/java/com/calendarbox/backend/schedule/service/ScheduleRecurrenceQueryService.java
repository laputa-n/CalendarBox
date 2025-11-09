package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import com.calendarbox.backend.schedule.dto.response.RecurrenceResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleRecurrenceQueryService {
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final CalendarMemberRepository calendarMemberRepository;

    public List<RecurrenceResponse> list(Long userId, Long scheduleId) {
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar c = s.getCalendar();
        if(!s.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),userId, CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return scheduleRecurrenceRepository.findBySchedule_Id(scheduleId).stream()
                .map(this::toResponse).toList();
    }

    public RecurrenceResponse getOne(Long userId, Long scheduleId, Long recurrenceId) {
        var r = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        Schedule s = r.getSchedule();
        Calendar c = s.getCalendar();
        if(!s.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),userId, CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if (!r.getSchedule().getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_RECUR_EXDATE_MISMATCH);
        return toResponse(r);
    }

    private RecurrenceResponse  toResponse(ScheduleRecurrence r) {
        List<LocalDate> exDates = new ArrayList<>();
        for(var ex:r.getExceptions()){
            exDates.add(ex.getExceptionDate());
        }
        return new RecurrenceResponse(
                r.getId(), r.getFreq(), r.getIntervalCount(),
                r.getByDay()==null ? List.of() : Arrays.asList(r.getByDay()),
                r.getByMonthday()==null ? List.of() : Arrays.asList(r.getByMonthday()),
                r.getByMonth()==null ? List.of() : Arrays.asList(r.getByMonth()),
                r.getUntil(), exDates, r.getCreatedAt()
        );
    }
}

