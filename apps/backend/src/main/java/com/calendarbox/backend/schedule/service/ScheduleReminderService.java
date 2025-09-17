package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleReminder;
import com.calendarbox.backend.schedule.dto.request.ReminderRequest;
import com.calendarbox.backend.schedule.dto.response.ReminderResponse;
import com.calendarbox.backend.schedule.repository.ScheduleReminderRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleReminderService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleReminderRepository scheduleReminderRepository;
    public ReminderResponse create(Long userId, Long scheduleId, ReminderRequest req) {
        //userId 검증 필요
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow
                (() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(scheduleReminderRepository.findAllMinutesBeforeBySchedule_Id(scheduleId).contains(req.minutesBefore())){
            throw new BusinessException(ErrorCode.REMINDER_MINUTES_DUP);
        }
        ScheduleReminder r = ScheduleReminder.create(s, req.minutesBefore());

        scheduleReminderRepository.save(r);

        return new ReminderResponse(r.getId(), r.getMinutesBefore());
    }

    public void delete(Long userId, Long scheduleId, Long reminderId) {
        var r = scheduleReminderRepository.findById(reminderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        scheduleReminderRepository.delete(r);
    }
}
