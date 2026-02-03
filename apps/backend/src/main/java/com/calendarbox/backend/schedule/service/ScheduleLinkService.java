package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleLink;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleLinkRequest;
import com.calendarbox.backend.schedule.dto.response.ScheduleLinkDto;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleLinkRepository;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Transactional
public class ScheduleLinkService {
    private final MemberRepository memberRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleLinkRepository scheduleLinkRepository;
    private final ScheduleRepository scheduleRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public ScheduleLinkDto add(Long userId, Long scheduleId, CreateScheduleLinkRequest request) {
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!schedule.getCreatedBy().getId().equals(userId)
        && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if(scheduleLinkRepository.existsBySchedule_IdAndUrl(schedule.getId(),request.url())) throw new BusinessException(ErrorCode.SCHEDULE_LINK_ALREADY_EXISTS);
        String label = request.label() == null? request.url() : request.label();
        if(scheduleLinkRepository.existsBySchedule_IdAndLabel(schedule.getId(),label)) throw new BusinessException(ErrorCode.SCHEDULE_LINK_LABEL_DUP);

        ScheduleLink link = ScheduleLink.of(request.url(),label);
        schedule.addLink(link);

        scheduleRepository.flush();

        return new ScheduleLinkDto(
                link.getId(),
                schedule.getId(),
                link.getUrl(),
                link.getLabel(),
                link.getCreatedAt()
        );
    }
    public void delete(Long userId, Long scheduleId, Long linkId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        ScheduleLink link = scheduleLinkRepository.findById(linkId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_LINK_NOT_FOUND));
        Schedule schedule = link.getSchedule();

        if(!schedule.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        schedule.removeLink(link);

    }
}
