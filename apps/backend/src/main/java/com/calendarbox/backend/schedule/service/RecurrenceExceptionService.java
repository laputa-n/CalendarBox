package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrenceException;
import com.calendarbox.backend.schedule.dto.request.RecurrenceExceptionRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceExceptionResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceExceptionRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RecurrenceExceptionService {
    private final ScheduleRecurrenceExceptionRepository scheduleRecurrenceExceptionRepository;
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    public RecurrenceExceptionResponse add(Long userId, Long recurrenceId, RecurrenceExceptionRequest req) {
        var recurrence = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));

        Schedule s = recurrence.getSchedule();
        if(!s.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if (scheduleRecurrenceExceptionRepository.existsByScheduleRecurrence_IdAndExceptionDate(recurrence.getId(), req.exceptionDate()))
            throw new BusinessException(ErrorCode.RECURRENCE_EXDATE_DUP);

        var ex = ScheduleRecurrenceException.of(req.exceptionDate());
        recurrence.addException(ex);

        scheduleRecurrenceRepository.save(recurrence);

        return new RecurrenceExceptionResponse(ex.getId(), ex.getExceptionDate());
    }

    public void delete(Long userId, Long exceptionId) {
        var e = scheduleRecurrenceExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_EXDATE_NOT_FOUND));
        var r = e.getScheduleRecurrence();
        var s = r.getSchedule();
        if(!s.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        r.removeException(e);
    }
}

