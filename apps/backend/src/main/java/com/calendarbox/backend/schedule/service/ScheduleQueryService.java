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
import com.calendarbox.backend.schedule.dto.response.ScheduleListItem;
import com.calendarbox.backend.schedule.dto.response.ScheduleListResponse;
import com.calendarbox.backend.schedule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
    private final CalendarRepository calendarRepository;
    public ScheduleDetailDto getDetail(Long userId, Long scheduleId) {
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Calendar c = s.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),user.getId(), CalendarMemberStatus.ACCEPTED) && !c.getOwner().getId().equals(user.getId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Long cntParticipants = scheduleParticipantRepository.countBySchedule_Id(s.getId());
        Long cntRecurrences = scheduleRecurrenceRepository.countBySchedule_Id(s.getId());
        Long cntReminders = scheduleReminderRepository.countBySchedule_Id(s.getId());
        Long cntLinks = scheduleLinkRepository.countBySchedule_Id(s.getId());
        Long cntTodos = scheduleTodoRepository.countBySchedule_Id(s.getId());
        Long cntPlaces = schedulePlaceRepository.countBySchedule_Id(s.getId());
        Long cntImgs = attachmentRepository.countBySchedule_IdAndIsImg(s.getId(), true);
        Long cntFiles = attachmentRepository.countBySchedule_IdAndIsImg(s.getId(), false);

        return ScheduleDetailDto.of(s, ScheduleDetailSummary.of(
                cntParticipants > 0, cntParticipants,
                cntRecurrences > 0, cntRecurrences,
                cntReminders > 0, cntReminders,
                cntLinks > 0, cntLinks,
                cntTodos > 0, cntTodos,
                cntPlaces > 0, cntPlaces,
                cntImgs > 0, cntImgs,
                cntFiles > 0, cntFiles
        ));
    }

    public Page<ScheduleListItem> getList(Long userId, Long calendarId, Instant from, Instant to, Pageable pageable){
        Member viewer = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> effectiveIds = new ArrayList<>();
        if(calendarId != null) effectiveIds.add(calendarId);

        if (effectiveIds.isEmpty()) {
            effectiveIds = calendarMemberRepository.findCalendarIdsByMemberIdAndStatuses(viewer.getId(), List.of(CalendarMemberStatus.ACCEPTED));
            if (effectiveIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }
        Page<Schedule> page;

        boolean hasFrom = (from != null);
        boolean hasTo   = (to   != null);

        if (hasFrom && !hasTo) {
            page = scheduleRepository.findAllFrom(effectiveIds, from,pageable);
        } else if (!hasFrom && hasTo) {
            page = scheduleRepository.findAllUntil(effectiveIds, to, pageable);
        } else if (hasFrom && hasTo) {
            if (!from.isBefore(to)) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
            page = scheduleRepository.findAllOverlapping(effectiveIds, from, to,pageable);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        return page.map(s -> new ScheduleListItem(
                s.getCalendar().getId(),
                s.getCalendar().getType(),
                s.getCalendar().getName(),
                s.getId(),
                s.getTitle(),
                s.getStartAt(),
                s.getEndAt()
        ));
    }

    public ScheduleListResponse search(Long userId, List<Long> calendarIds, String query){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> requestedIds = (calendarIds == null)? List.of() : new ArrayList<>(new HashSet<>(calendarIds));
        List<Long> effectiveIds;
        if (requestedIds.isEmpty()) {
            effectiveIds = calendarMemberRepository.findCalendarIdsByMemberIdAndStatuses(
                    user.getId(),List.of(CalendarMemberStatus.ACCEPTED)
            );
            if (effectiveIds.isEmpty()) {
                return new ScheduleListResponse(0, List.of());
            }
        } else {
            for (Long cId : requestedIds) {
                Calendar c = calendarRepository.findById(cId).orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));
                if (!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),user.getId(),CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
            }
            effectiveIds = requestedIds;
        }

       List<Schedule> schedules =  scheduleRepository.searchByKeyword(effectiveIds, query);

        List<ScheduleListItem> items = schedules.stream()
                .map(s -> new ScheduleListItem(
                        s.getCalendar().getId(),
                        s.getCalendar().getType(),
                        s.getCalendar().getName(),
                        s.getId(),
                        s.getTitle(),
                        s.getStartAt(),
                        s.getEndAt()
                ))
                .toList();

        return new ScheduleListResponse(items.size(), items);
    }

    private Pageable fixSort(Pageable p) {
        int page = (p == null) ? 0 : p.getPageNumber();
        int size = (p == null) ? 10 : p.getPageSize();
        return PageRequest.of(page, size);
    }
}
