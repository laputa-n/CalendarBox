package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.dto.request.CloneScheduleRequest;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.CloneScheduleResponse;
import com.calendarbox.backend.schedule.dto.response.CreateScheduleResponse;
import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import com.calendarbox.backend.schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleLinkRepository scheduleLinkRepository;
    private final ScheduleTodoRepository scheduleTodoRepository;
    private final SchedulePlaceRepository schedulePlaceRepository;
    private final AttachmentRepository attachmentRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarMemberRepository calendarMemberRepository;


    public CloneScheduleResponse clone(Long userId, Long calendarId, CloneScheduleRequest request) {
        if (request == null || request.sourceScheduleId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Member creator = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Calendar targetCalendar = calendarRepository.findById(calendarId).orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        //권한 체크 넣어야 함

        Schedule src = scheduleRepository.findById(request.sourceScheduleId()).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Instant reqStart = request.startAt();
        Instant reqEnd = request.endAt();

        final Instant effStart;
        final Instant effEnd;

        if (reqStart == null && reqEnd == null) {
            effStart = src.getStartAt();
            effEnd   = src.getEndAt();
        } else if (reqStart != null && reqEnd != null) {
            effStart = reqStart;
            effEnd   = reqEnd;
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        if(!effStart.isBefore(effEnd)) throw new BusinessException(ErrorCode.VALIDATION_ERROR);

        Schedule dst = Schedule.cloneHeader(src,targetCalendar,creator,effStart,effEnd);

        scheduleRepository.save(dst);

        Long srcId = src.getId();
        Long dstId = dst.getId();

        scheduleLinkRepository.copyAll(srcId,dstId);
        scheduleTodoRepository.copyAll(srcId,dstId);
        schedulePlaceRepository.copyAllForClone(srcId, dstId);
        attachmentRepository.copyAllDbOnly(srcId, dstId);

        return new CloneScheduleResponse(dstId);
    }

    public CreateScheduleResponse create(Long userId, Long calendarId, CreateScheduleRequest request) {
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),user.getId(), CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if(!request.startAt().isBefore(request.endAt())) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);

        ScheduleTheme theme = (request.theme() == null)? ScheduleTheme.BLACK : request.theme();
        Schedule schedule = new Schedule(calendar, request.title(), request.memo(), theme, request.startAt(),request.endAt(), user);
        scheduleRepository.save(schedule);

        return new CreateScheduleResponse(
                schedule.getCalendar().getId(),
                schedule.getId(),
                schedule.getTitle(),
                schedule.getMemo(),
                schedule.getTheme(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getCreatedBy().getId(),
                schedule.getCreatedAt()
        );
    }
}
