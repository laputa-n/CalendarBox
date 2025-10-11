package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.*;
import com.calendarbox.backend.schedule.dto.request.CloneScheduleRequest;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleRequest;
import com.calendarbox.backend.schedule.dto.request.EditScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.CloneScheduleResponse;
import com.calendarbox.backend.schedule.dto.response.CreateScheduleResponse;
import com.calendarbox.backend.schedule.dto.response.ScheduleDto;
import com.calendarbox.backend.schedule.enums.RecurrenceFreq;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import com.calendarbox.backend.schedule.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ObjectMapper objectMapper;


    public CloneScheduleResponse clone(Long userId, Long calendarId, CloneScheduleRequest request) {
        if (request == null || request.sourceScheduleId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Member creator = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Calendar targetCalendar = calendarRepository.findById(calendarId).orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        if(calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(targetCalendar.getId(), creator.getId(), CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Schedule src = scheduleRepository.findById(request.sourceScheduleId()).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(src.getCalendar().getId(), creator.getId(), CalendarMemberStatus.ACCEPTED) || !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(src.getId(), creator.getId(), ScheduleParticipantStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

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
        } else if (reqEnd != null){
            effStart = src.getStartAt();
            effEnd = reqEnd;
        } else {
            effStart = reqStart;
            effEnd = src.getEndAt();
        }

        if(!effStart.isBefore(effEnd)) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);

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


        // 1. 링크
        if(request.links() != null) {
            for(var l : request.links()) {
                schedule.addLink(ScheduleLink.of(l.url(), l.label()));
            }
        }

        // 2. 투두
        if(request.todos() != null){
            for(var t:request.todos()){
                schedule.addTodo(ScheduleTodo.of(t.content(),t.isDone(),t.orderNo()));
            }
        }

        // 3. 리마인더
        if(request.reminders() != null){
            for(var r:request.reminders()){
                schedule.addReminder(ScheduleReminder.of(r.minutesBefore()));
            }
        }

        // 4. 반복
        if (request.recurrence() != null) {
            var r = request.recurrence();

            if (!r.until().isAfter(schedule.getEndAt())) {
                throw new BusinessException(ErrorCode.RECURRENCE_UNTIL_BEFORE_END);
            }

            Set<String> byDay = r.byDay();
            if (r.freq() == RecurrenceFreq.WEEKLY && (byDay == null || byDay.isEmpty())) {
                var zone = ZoneId.of("Asia/Seoul");
                var dow = schedule.getStartAt().atZone(zone).getDayOfWeek();
                String token = switch (dow) {
                    case MONDAY -> "MO";
                    case TUESDAY -> "TU";
                    case WEDNESDAY -> "WE";
                    case THURSDAY -> "TH";
                    case FRIDAY -> "FR";
                    case SATURDAY -> "SA";
                    case SUNDAY -> "SU";
                };
                byDay = Set.of(token);
            }

            String[] byDayArr = (byDay == null) ? null :
                    byDay.stream().filter(Objects::nonNull).map(s -> s.trim().toUpperCase())
                            .distinct().sorted().toArray(String[]::new);
            Integer[] byMonthdayArr = (r.byMonthday() == null) ? null :
                    r.byMonthday().stream().distinct().sorted().toArray(Integer[]::new);
            Integer[] byMonthArr = (r.byMonth() == null) ? null :
                    r.byMonth().stream().distinct().sorted().toArray(Integer[]::new);

            ScheduleRecurrence sr = ScheduleRecurrence.of(
                    r.freq(), r.intervalCount(), byDayArr, byMonthdayArr, byMonthArr, r.until()
            );

            // 예외일 추가
            if (r.exceptions() != null && !r.exceptions().isEmpty()) {
                var uniq = r.exceptions().stream().distinct().sorted().toList();
                for (var exDate : uniq) {
                    sr.addException(ScheduleRecurrenceException.of(exDate));
                }
            }

            // Schedule 애그리거트로 편입
            schedule.addRecurrence(sr);
        }
        // 5. 참가자
        // 6. 장소
        // 7. 첨부

        scheduleRepository.save(schedule);
        CalendarHistory history = CalendarHistory.builder()
                .calendar(calendar)
                .actor(user)
                .entityId(schedule.getId())
                .type(CalendarHistoryType.SCHEDULE_CREATED)
                .build();
        calendarHistoryRepository.save(history);

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

    public ScheduleDto edit(Long userId, Long scheduleId, EditScheduleRequest request){
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Calendar c = s.getCalendar();

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),user.getId(),CalendarMemberStatus.ACCEPTED) && !c.getOwner().getId().equals(user.getId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);


        Instant newStart = request.startAt().orElse(s.getStartAt());
        Instant newEnd   = request.endAt().orElse(s.getEndAt());
        if (!newStart.isBefore(newEnd)) {
            throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
        }
        Map<String,Object> diff = new HashMap<>();

        request.title().ifPresent(title -> {
            String oldTitle = s.getTitle();
            s.editTitle(title);
            diff.put("title", Map.of("old", oldTitle, "new", title));
        });
        request.memo().ifPresent(memo -> {
            String oldMemo = s.getMemo();
            s.editMemo(memo);
            diff.put("memo", Map.of("old", oldMemo, "new", memo));
        });
        request.theme().ifPresent(theme ->{
            ScheduleTheme oldTheme = s.getTheme();
            s.editTheme(theme);
            diff.put("theme", Map.of("old", oldTheme, "new", theme));
        });
        if(request.startAt().isPresent() || request.endAt().isPresent()){
            Instant oldStartAt = s.getStartAt();
            Instant oldEndAt   = s.getEndAt();
            diff.put("time",Map.of("oldStartAt",oldStartAt,"oldEndAt",oldEndAt,
                                    "newStartAt", newStart, "newEndAt",newEnd));
        }
        s.reschedule(newStart, newEnd);
        s.touchUpdateBy(user);

        if(!s.getStartAt().isBefore(s.getEndAt())) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(c)
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_UPDATED)
                .changedFields(toJson(diff))
                .build();
        calendarHistoryRepository.save(history);
        return new ScheduleDto(
                s.getId(), s.getCalendar().getId(), s.getTitle(), s.getMemo(), s.getTheme()
                , s.getStartAt(), s.getEndAt(), s.getCreatedBy().getId(), s.getUpdatedBy().getId()
                , s.getCreatedAt(), s.getUpdatedAt()
        );
    }

    public void delete(Long userId, Long scheduleId){
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Calendar c = s.getCalendar();

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),user.getId(),CalendarMemberStatus.ACCEPTED) && !c.getOwner().getId().equals(user.getId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);


        CalendarHistory history = CalendarHistory.builder()
                .calendar(c)
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_DELETED)
                .build();
        calendarHistoryRepository.save(history);
        scheduleRepository.delete(s);
    }
    private String toJson(Object value){
        try{
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
}
