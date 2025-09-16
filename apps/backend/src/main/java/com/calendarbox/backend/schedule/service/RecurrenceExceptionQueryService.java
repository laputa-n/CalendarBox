package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.schedule.dto.response.RecurrenceExceptionResponse;
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

    public List<RecurrenceExceptionResponse> list(Long userId, Long recurrenceId) {
        return scheduleRecurrenceExceptionRepository.findByRecurrence_Id(recurrenceId).stream()
                .map(e -> new RecurrenceExceptionResponse(e.getId(), e.getExceptionDate()))
                .toList();
    }
}

