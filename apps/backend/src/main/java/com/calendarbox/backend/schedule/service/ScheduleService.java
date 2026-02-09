package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.enums.NotificationType;
import com.calendarbox.backend.notification.repository.NotificationRepository;
import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.place.repository.PlaceRepository;
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
import com.calendarbox.backend.schedule.util.DefaultScheduleEmbeddingService;
import com.calendarbox.backend.schedule.util.EmbeddingEnqueueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus.INVITED;

@Slf4j
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
    private final FriendshipRepository friendshipRepository;
    private final NotificationRepository notificationRepository;
    private final PlaceRepository placeRepository;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;
    private final ScheduleEmbeddingRepository scheduleEmbeddingRepository;
    private final EmbeddingEnqueueService embeddingEnqueueService;


    public CloneScheduleResponse clone(Long userId, Long calendarId, CloneScheduleRequest request) {
        if (request == null || request.sourceScheduleId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Member creator = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Calendar targetCalendar = calendarRepository.findById(calendarId).orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(targetCalendar.getId(), creator.getId(), CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Schedule src = scheduleRepository.findById(request.sourceScheduleId()).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(src.getCalendar().getId(), creator.getId(), CalendarMemberStatus.ACCEPTED) || !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(src.getId(), creator.getId(), ScheduleParticipantStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        ZoneId zone = ZoneId.of("Asia/Seoul");

        ZonedDateTime srcStartKst = src.getStartAt().atZone(zone);
        ZonedDateTime srcEndKst   = src.getEndAt().atZone(zone);

        if (!srcStartKst.isBefore(srcEndKst)) {
            throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
        }

        Duration dur = Duration.between(srcStartKst, srcEndKst);

        ZonedDateTime startZ = request.targetDate().atTime(srcStartKst.toLocalTime()).atZone(zone);

        ZonedDateTime endZ = startZ.plus(dur);

        Instant effStart = startZ.toInstant();
        Instant effEnd = endZ.toInstant();

        if (!effStart.isBefore(effEnd)) {
            throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
        }

        Schedule dst = Schedule.cloneHeader(src,targetCalendar,creator,effStart,effEnd);
        scheduleRepository.save(dst);

        Long srcId = src.getId();
        Long dstId = dst.getId();

        scheduleLinkRepository.copyAll(srcId,dstId);
        scheduleTodoRepository.copyAll(srcId,dstId);
        schedulePlaceRepository.copyAllForClone(srcId, dstId);
        attachmentRepository.copyAllDbOnly(srcId, dstId);

        scheduleEmbeddingRepository.copyFrom(srcId, dstId);

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

            if (r.exceptions() != null && !r.exceptions().isEmpty()) {
                var uniq = r.exceptions().stream().distinct().sorted().toList();
                for (var exDate : uniq) {
                    sr.addException(ScheduleRecurrenceException.of(exDate));
                }
            }

            schedule.makeRecurrence(sr);
        }

        // 5. 참가자
        if(request.participants() != null) {
            for (var participant : request.participants()) {
                switch(participant.mode()) {
                    case SERVICE_USER -> {
                        Member addressee = memberRepository.findById(participant.memberId()).orElseThrow(() ->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

                        ScheduleParticipant sp = ScheduleParticipant.ofMember(null,addressee,user);
                        schedule.addParticipant(sp);
                    }
                    case NAME -> {
                        String normalized = participant.name().trim();
                        ScheduleParticipant sp = ScheduleParticipant.ofName(null,normalized, user);
                        schedule.addParticipant(sp);
                    }
                }
            }
        }

        // 6. 장소
        if (request.places() != null && !request.places().isEmpty()) {
            int nextPos = (schedule.getPlaces() == null) ? 0 : schedule.getPlaces().size();

            Set<String> nameSet = new HashSet<>();
            Set<Long> placeIdSet = new HashSet<>();

            for (var p : request.places()) {
                switch (p.mode()) {
                    case MANUAL -> {
                        String normalized = normalize(p.name());
                        if (normalized == null || normalized.isEmpty()) {
                            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_NAME_NEED);
                        }

                        if (!nameSet.add(normalized)) {
                            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_NAME_DUP);
                        }
                        SchedulePlace sp = SchedulePlace.builder()
                                .name(normalized)
                                .position(nextPos++)
                                .build();
                        schedule.addPlace(sp);
                    }

                    case EXISTING -> {
                        if (p.placeId() == null) {
                            throw new BusinessException(ErrorCode.EXISTING_PLACE_ID_NEED);
                        }
                        Place place = placeRepository.findById(p.placeId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

                        if (!placeIdSet.add(place.getId())) {
                            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_DUP);
                        }

                        String n = hasText(p.name()) ? p.name().trim() : p.title();
                        String normalized = normalize(n);

                        SchedulePlace sp = SchedulePlace.builder()
                                .place(place)
                                .name(normalized)
                                .position(nextPos++)
                                .build();
                        schedule.addPlace(sp);
                    }

                    case PROVIDER -> {
                        if (!hasText(p.provider()) || !hasText(p.providerPlaceKey())) {
                            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                        }
                        Place place = placeRepository.findByProviderAndProviderPlaceKey(p.provider(), p.providerPlaceKey())
                                .orElseGet(() -> {
                                    // Double -> BigDecimal 변환
                                    BigDecimal lat = (p.lat() != null) ? BigDecimal.valueOf(p.lat()) : null;
                                    BigDecimal lng = (p.lng() != null) ? BigDecimal.valueOf(p.lng()) : null;

                                    Place np = Place.builder()
                                            .provider(p.provider())
                                            .providerPlaceKey(p.providerPlaceKey())
                                            .title(p.title())
                                            .link(p.link())
                                            .category(p.category())
                                            .description(p.description())
                                            .address(p.address())
                                            .roadAddress(p.roadAddress())
                                            .lat(lat)
                                            .lng(lng)
                                            .build();
                                    return placeRepository.save(np);
                                });

                        if (!placeIdSet.add(place.getId())) {
                            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_DUP);
                        }

                        String n = hasText(p.name()) ? p.name().trim() : p.title();
                        String normalized = normalize(n);

                        SchedulePlace sp = SchedulePlace.builder()
                                .place(place)
                                .name(normalized)
                                .position(nextPos++)
                                .build();
                        schedule.addPlace(sp);
                    }
                }
            }
        }

        // 7. 첨부 -> 스케줄 만들고 차례로 upload

        scheduleRepository.save(schedule);
        scheduleRepository.flush();

        embeddingEnqueueService.enqueueAfterCommit(schedule.getId());

        List<Notification> inviteNotis = schedule.getParticipants().stream()
                .filter(sp -> sp.getMember() != null)
                .filter(sp -> sp.getStatus() == INVITED)
                .map(sp -> Notification.builder()
                        .member(sp.getMember())
                        .actor(user)
                        .type(NotificationType.INVITED_TO_SCHEDULE)
                        .resourceId(sp.getId())
                        .payloadJson(Map.of(
                                "scheduleId", schedule.getId(),
                                "scheduleTitle", schedule.getTitle(),
                                "scheduleStartAt", schedule.getStartAt(),
                                "scheduleEndAt", schedule.getEndAt(),
                                "actorName", user.getName()
                        ))
                        .dedupeKey("scheduleInvite:" + sp.getId())       // 재발행 방지
                        .build())
                .toList();

        notificationRepository.saveAll(inviteNotis);
        CalendarHistory history = CalendarHistory.builder()
                .calendar(calendar)
                .actor(user)
                .entityId(schedule.getId())
                .type(CalendarHistoryType.SCHEDULE_CREATED)
                .changedFields(Map.of(
                        "title", schedule.getTitle(),
                        "startAt", schedule.getStartAt(),
                        "endAt", schedule.getEndAt()
                ))
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
                schedule.getCreatedAt(),

                schedule.getLinks().size(),
                schedule.getTodos().size(),
                schedule.getReminders().size(),
                schedule.getParticipants().size(),
                schedule.getPlaces().size(),
                (schedule.getRecurrence()!=null)
        );
    }

    public ScheduleDto edit(Long userId, Long scheduleId, EditScheduleRequest req){
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Calendar c = s.getCalendar();

        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),user.getId(),CalendarMemberStatus.ACCEPTED) && !c.getOwner().getId().equals(user.getId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);


        boolean changed = false;

        Instant newStart = (req.startAt() != null) ? req.startAt() : s.getStartAt();
        Instant newEnd   = (req.endAt()   != null) ? req.endAt()   : s.getEndAt();
        if (req.startAt() != null || req.endAt() != null) {
            if (!newStart.isBefore(newEnd)) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
            if (!newStart.equals(s.getStartAt()) || !newEnd.equals(s.getEndAt())) {
                s.reschedule(newStart, newEnd);
                changed = true;
            }
        }

        if (req.title() != null && !req.title().equals(s.getTitle())) {
            s.editTitle(req.title());
            changed = true;
        }
        if (req.memo() != null && !Objects.equals(req.memo(), s.getMemo())) {
            s.editMemo(req.memo());
            changed = true;
        }
        if (req.theme() != null && req.theme() != s.getTheme()) {
            s.editTheme(req.theme());
            changed = true;
        }

        if (changed) {
            s.touchUpdateBy(user);

            try {
                float[] embedding = scheduleEmbeddingService.embedScheduleEntity(s);
                scheduleEmbeddingRepository.upsertEmbedding(s.getId(), embedding);
            } catch (Exception e) {
                log.error("Failed to update schedule embedding. scheduleId={}", s.getId(), e);
            }
            calendarHistoryRepository.save(
                    CalendarHistory.builder()
                            .calendar(c)
                            .actor(user)
                            .entityId(s.getId())
                            .type(CalendarHistoryType.SCHEDULE_UPDATED)
                            .changedFields(Map.of(
                                    "title", s.getTitle(),
                                    "startAt", s.getStartAt(),
                                    "endAt", s.getEndAt()
                            ))
                            .build()
            );
        }

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
                .changedFields(Map.of("title",s.getTitle(),"startAt",s.getStartAt(),"endAt",s.getEndAt()))
                .build();
        calendarHistoryRepository.save(history);
        scheduleEmbeddingRepository.deleteByScheduleId(s.getId());
        scheduleRepository.delete(s);
    }


    private String toJson(Object value){
        try{
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
