package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.schedule.dto.response.ReminderResponse;
import com.calendarbox.backend.schedule.repository.ScheduleReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleReminderQueryService {
    private final ScheduleReminderRepository scheduleReminderRepository;

    public List<ReminderResponse> list(Long userId, Long scheduleId) {
        return scheduleReminderRepository.findAllBySchedule_Id(scheduleId).stream()
                .map(r -> new ReminderResponse(r.getId(), r.getMinutesBefore()))
                .toList();
    }
}
