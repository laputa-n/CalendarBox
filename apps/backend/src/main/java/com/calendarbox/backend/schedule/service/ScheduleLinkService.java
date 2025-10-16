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
import com.calendarbox.backend.schedule.repository.ScheduleLinkRepository;
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
    private final ObjectMapper objectMapper;
    public ScheduleLinkDto add(Long userId, Long scheduleId, CreateScheduleLinkRequest request) {
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(schedule.getCalendar().getId(),user.getId(), CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if(scheduleLinkRepository.existsBySchedule_IdAndUrl(schedule.getId(),request.url())) throw new BusinessException(ErrorCode.SCHEDULE_LINK_ALREADY_EXISTS);
        String label = request.label() == null? request.url() : request.label();
        if(scheduleLinkRepository.existsBySchedule_IdAndLabel(schedule.getId(),label)) throw new BusinessException(ErrorCode.SCHEDULE_LINK_LABEL_DUP);

        ScheduleLink link = ScheduleLink.of(request.url(),label);
        schedule.addLink(link);

        scheduleLinkRepository.save(link);
        scheduleLinkRepository.flush();


        calendarHistoryRepository.save(CalendarHistory.builder()
                .calendar(schedule.getCalendar())
                .actor(user)
                .entityId(scheduleId)
                .type(CalendarHistoryType.SCHEDULE_LINK_ADDED)
                .changedFields(Map.of(
                        "url", link.getUrl(),
                        "label", link.getLabel()
                ))
                .build()
        );

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
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        ScheduleLink link = scheduleLinkRepository.findById(linkId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_LINK_NOT_FOUND));

        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),user.getId(),CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        schedule.removeLink(link);

        calendarHistoryRepository.save(CalendarHistory.builder()
                .calendar(schedule.getCalendar())
                .actor(user)
                .entityId(scheduleId)
                .type(CalendarHistoryType.SCHEDULE_LINK_REMOVED)
                .changedFields(Map.of(
                        "url", link.getUrl(),
                        "label", link.getLabel()
                ))
                .build()
        );
    }
}
