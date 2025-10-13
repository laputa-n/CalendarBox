package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleParticipant;
import com.calendarbox.backend.schedule.dto.response.ScheduleParticipantResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleParticipantQueryService {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;

    public Page<ScheduleParticipantResponse> list(
            Long userId, ScheduleParticipantStatus status, Long scheduleId, Pageable pageable
    ){
        Member viewer = memberRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(()->new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(), viewer.getId(), CalendarMemberStatus.ACCEPTED) &&
                !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatusIn(schedule.getId(), viewer.getId(), List.of(ScheduleParticipantStatus.ACCEPTED, ScheduleParticipantStatus.INVITED)))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Page<ScheduleParticipant> splist = scheduleParticipantRepository.findAllByScheduleAndStatus(scheduleId,status, pageable);
        return splist.map(sp -> toResponse(sp,scheduleId));
    }

    private ScheduleParticipantResponse toResponse(ScheduleParticipant sp, Long scheduleId) {
        Long memberId = (sp.getMember() != null) ? sp.getMember().getId() : null;
        return new ScheduleParticipantResponse(
                sp.getId(),
                scheduleId,
                memberId,
                sp.getName(),
                sp.getInvitedAt(),
                sp.getRespondedAt(),
                sp.getStatus()
        );
    }
}
