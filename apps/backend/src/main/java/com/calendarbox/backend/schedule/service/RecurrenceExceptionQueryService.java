package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.dto.response.RecurrenceExceptionResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceExceptionRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecurrenceExceptionQueryService {
    private final ScheduleRecurrenceExceptionRepository scheduleRecurrenceExceptionRepository;
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public List<RecurrenceExceptionResponse> list(Long userId, Long recurrenceId) {
        var r = scheduleRecurrenceRepository.findById(recurrenceId).orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        Schedule s = r.getSchedule();
        Calendar c = s.getCalendar();
        if(!s.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),userId, CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return scheduleRecurrenceExceptionRepository.findByScheduleRecurrence_Id(recurrenceId).stream()
                .map(e -> new RecurrenceExceptionResponse(e.getId(), e.getExceptionDate()))
                .toList();
    }
}

