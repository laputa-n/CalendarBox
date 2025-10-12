package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleLink;
import com.calendarbox.backend.schedule.dto.response.ScheduleLinkDto;
import com.calendarbox.backend.schedule.repository.ScheduleLinkRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleLinkQueryService {
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleLinkRepository scheduleLinkRepository;
    public List<ScheduleLinkDto> getLinks(Long userId, Long scheduleId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(schedule.getCalendar().getId(),user.getId(), CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

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
