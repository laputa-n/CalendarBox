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
import com.calendarbox.backend.schedule.dto.response.ScheduleDetailDto;
import com.calendarbox.backend.schedule.dto.response.ScheduleDetailSummary;
import com.calendarbox.backend.schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleQueryService {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleLinkRepository scheduleLinkRepository;
    private final SchedulePlaceRepository schedulePlaceRepository;
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;
    private final ScheduleReminderRepository scheduleReminderRepository;
    private final ScheduleTodoRepository scheduleTodoRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final AttachmentRepository attachmentRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    public ScheduleDetailDto getDetail(Long userId, Long scheduleId) {
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Calendar c = s.getCalendar();

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),user.getId(), CalendarMemberStatus.ACCEPTED) && !c.getOwner().getId().equals(user.getId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        boolean hasParticipant = scheduleParticipantRepository.existsBySchedule_Id(s.getId());
        boolean hasRecurrence = scheduleRecurrenceRepository.existsBySchedule_Id(s.getId());
        boolean hasReminder = scheduleReminderRepository.existsBySchedule_Id(s.getId());
        boolean hasLink = scheduleLinkRepository.existsBySchedule_Id(s.getId());
        boolean hasTodo = scheduleTodoRepository.existsBySchedule_Id(s.getId());
        boolean hasPlace = schedulePlaceRepository.existsBySchedule_Id(s.getId());
        boolean hasImg = attachmentRepository.existsBySchedule_IdAndIsImg(s.getId(), true);
        boolean hasFile = attachmentRepository.existsBySchedule_IdAndIsImg(s.getId(), false);

        Long cntParticipants = scheduleParticipantRepository.countBySchedule_Id(s.getId());
        Long cntRecurrences = scheduleRecurrenceRepository.countBySchedule_Id(s.getId());
        Long cntReminders = scheduleReminderRepository.countBySchedule_Id(s.getId());
        Long cntLinks = scheduleLinkRepository.countBySchedule_Id(s.getId());
        Long cntTodos = scheduleTodoRepository.countBySchedule_Id(s.getId());
        Long cntPlaces = schedulePlaceRepository.countBySchedule_Id(s.getId());
        Long cntImgs = attachmentRepository.countBySchedule_IdAndIsImg(s.getId(), true);
        Long cntFiles = attachmentRepository.countBySchedule_IdAndIsImg(s.getId(), false);

        return ScheduleDetailDto.of(s, ScheduleDetailSummary.of(
                hasParticipant, cntParticipants,
                hasRecurrence, cntRecurrences,
                hasReminder, cntReminders,
                hasLink, cntLinks,
                hasTodo, cntTodos,
                hasPlace, cntPlaces,
                hasImg, cntImgs,
                hasFile, cntFiles
        ));
    }
}
