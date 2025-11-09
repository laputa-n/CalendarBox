package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleLink;
import com.calendarbox.backend.schedule.dto.response.ScheduleLinkDto;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleLinkRepository;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleLinkQueryService {
    private final ScheduleRepository scheduleRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleLinkRepository scheduleLinkRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public List<ScheduleLinkDto> getLinks(Long userId, Long scheduleId){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar calendar = schedule.getCalendar();
        if(!schedule.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED)
                &&!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId,CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        List<ScheduleLink> links = scheduleLinkRepository.findAllBySchedule_IdOrderByCreatedAtAscIdAsc(schedule.getId());

        return links.stream().map(l -> new ScheduleLinkDto(
                l.getId(),
                schedule.getId(),
                l.getUrl(),
                l.getLabel(),
                l.getCreatedAt()
        )).toList();
    }
}
