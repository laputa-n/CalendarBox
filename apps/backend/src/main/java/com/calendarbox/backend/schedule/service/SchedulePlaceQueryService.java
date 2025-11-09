package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.place.repository.PlaceRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.SchedulePlace;
import com.calendarbox.backend.schedule.dto.response.SchedulePlaceDto;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.SchedulePlaceRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulePlaceQueryService {

    private final SchedulePlaceRepository schedulePlaceRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;

    public SchedulePlaceDto getDetail(Long userId, Long scheduleId, Long schedulePlaceId) {
        SchedulePlace sp =  schedulePlaceRepository.findById(schedulePlaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PLACE_NOT_FOUND));
        Schedule schedule = sp.getSchedule();
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId, CalendarMemberStatus.ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatusIn(scheduleId,userId, List.of(ScheduleParticipantStatus.ACCEPTED,ScheduleParticipantStatus.INVITED)))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return SchedulePlaceDto.of(sp);
    }

    public List<SchedulePlaceDto> getSchedulePlaces(Long userId,Long scheduleId){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId, CalendarMemberStatus.ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED)
                && !schedule.getCreatedBy().getId().equals(userId))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return schedulePlaceRepository.findAllByScheduleIdOrderByPositionAsc(scheduleId).stream()
                .map(SchedulePlaceDto::of)
                .toList();
    }
}
