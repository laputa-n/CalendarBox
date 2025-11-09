package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.dto.response.TodoResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.calendarbox.backend.schedule.repository.ScheduleTodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleTodoQueryService {
    private final ScheduleTodoRepository todoRepo;
    private final ScheduleRepository scheduleRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public List<TodoResponse> list(Long userId, Long scheduleId) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!s.getCreatedBy().getId().equals(userId)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(s.getCalendar().getId(),userId,ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return todoRepo.findByScheduleIdOrderByOrder(scheduleId).stream()
                .map(TodoResponse::from)
                .toList();
    }
}
