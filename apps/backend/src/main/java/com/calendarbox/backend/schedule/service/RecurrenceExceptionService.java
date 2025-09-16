package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrenceException;
import com.calendarbox.backend.schedule.dto.request.RecurrenceExceptionRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceExceptionResponse;
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

    public RecurrenceExceptionResponse add(Long userId, Long recurrenceId, RecurrenceExceptionRequest req) {
        var recurrence = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        if (scheduleRecurrenceExceptionRepository.existsByRecurrence_IdAndExceptionDate(recurrence.getId(), req.exceptionDate()))
            throw new BusinessException(ErrorCode.RECURRENCE_EXDATE_DUP);

        var saved = scheduleRecurrenceExceptionRepository.save(ScheduleRecurrenceException.of(recurrence, req.exceptionDate()));
        return new RecurrenceExceptionResponse(saved.getId(), saved.getExceptionDate());
    }

    public void delete(Long userId, Long exceptionId) {
        var e = scheduleRecurrenceExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_EXDATE_NOT_FOUND));
        scheduleRecurrenceExceptionRepository.delete(e);
    }
}

