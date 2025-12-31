package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleReminder;
import com.calendarbox.backend.schedule.dto.request.ReminderRequest;
import com.calendarbox.backend.schedule.dto.response.ReminderResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
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
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    public ReminderResponse create(Long userId, Long scheduleId, ReminderRequest req) {
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow
                (() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(scheduleReminderRepository.existsByScheduleIdAndMinutesBefore(scheduleId,req.minutesBefore())){
            throw new BusinessException(ErrorCode.REMINDER_MINUTES_DUP);
        }
        if(!s.getCreatedBy().getId().equals(userId)
            && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        ScheduleReminder r = ScheduleReminder.create(s, req.minutesBefore());
        scheduleReminderRepository.save(r);
        scheduleReminderRepository.flush();

        return new ReminderResponse(r.getId(), r.getMinutesBefore());
    }

    public void delete(Long userId, Long scheduleId, Long reminderId) {
        var r = scheduleReminderRepository.findById(reminderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        Schedule s = r.getSchedule();
        if(!s.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        s.removeReminder(r);
    }
}
