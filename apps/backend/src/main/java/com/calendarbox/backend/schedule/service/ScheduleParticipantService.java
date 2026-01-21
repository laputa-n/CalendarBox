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
import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.enums.NotificationType;
import com.calendarbox.backend.notification.repository.NotificationRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleParticipant;
import com.calendarbox.backend.schedule.dto.request.AddParticipantRequest;
import com.calendarbox.backend.schedule.dto.request.ParticipantRespondRequest;
import com.calendarbox.backend.schedule.dto.response.AddParticipantResponse;
import com.calendarbox.backend.schedule.dto.response.ScheduleParticipantResponse;
import com.calendarbox.backend.schedule.repository.ScheduleEmbeddingRepository;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.calendarbox.backend.schedule.util.DefaultScheduleEmbeddingService;
import com.calendarbox.backend.schedule.util.EmbeddingEnqueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus.ACCEPTED;
import static com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus.INVITED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleParticipantService {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final NotificationRepository notificationRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final DefaultScheduleEmbeddingService scheduleEmbeddingService;
    private final ScheduleEmbeddingRepository scheduleEmbeddingRepository;
    private final EmbeddingEnqueueService embeddingEnqueueService;

    public AddParticipantResponse add(Long userId, Long scheduleId, AddParticipantRequest request) {

        return switch(request.mode()){
            case SERVICE_USER -> addServiceUser(userId, scheduleId, request.memberId());
            case NAME -> addName(userId,scheduleId,request.name());
        };
    }

    public void remove(Long userId, Long scheduleId, Long participantId) {

        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar c = s.getCalendar();
        ScheduleParticipant sp = scheduleParticipantRepository
                .findByIdAndSchedule_Id(participantId, scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));
        //권한 체크
        boolean isOwner = s.getCreatedBy().getId().equals(userId);
        boolean isInviter = sp.getInviter() != null && sp.getInviter().getId().equals(userId);
        boolean isSelf = sp.getMember() != null && sp.getMember().getId().equals(userId);

        if (sp.getMember() == null) {
            if (!isOwner) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        } else if (isSelf) {
            if (!sp.getStatus().equals(ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        } else {
            if (sp.getStatus().equals(ACCEPTED)) {
                if (!isOwner) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

            } else if (sp.getStatus().equals(INVITED)) {
                // ✅ inviter가 null이면 isInviter=false라서 owner만 가능
                if (!isOwner && !isInviter) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

            } else {
                // ✅ 나머지 상태는 명시적으로 차단
                throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
            }
        }


        s.removeParticipant(sp);

        Map<String, Object> removedParticipant = new HashMap<>();
        removedParticipant.put("removedParticipantName", sp.getName());

        embeddingEnqueueService.enqueueAfterCommit(s.getId());

        CalendarHistory history = CalendarHistory.builder()
                .calendar(c)
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_PARTICIPANT_REMOVED)
                .changedFields(removedParticipant)
                .build();
        calendarHistoryRepository.save(history);
    }

    public ScheduleParticipantResponse respond(Long userId, Long scheduleId, Long participantId, ParticipantRespondRequest request){
        ScheduleParticipant sp = scheduleParticipantRepository.findById(participantId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));
        if(!Objects.equals(sp.getMember().getId(), userId)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(sp.getStatus() != INVITED) return toResponse(sp);

        switch(request.action()){
            case ACCEPT -> {
                Map<String, Object> newParticipant = new HashMap<>();
                newParticipant.put("newParticipantName", sp.getName());

                CalendarHistory history = CalendarHistory.builder()
                        .calendar(s.getCalendar())
                        .actor(sp.getMember())
                        .entityId(s.getId())
                        .type(CalendarHistoryType.SCHEDULE_PARTICIPANT_ADDED)
                        .changedFields(newParticipant)
                        .build();
                calendarHistoryRepository.save(history);
                sp.accept();

                embeddingEnqueueService.enqueueAfterCommit(s.getId());
            }
            case REJECT -> sp.decline();
        }
        return toResponse(sp);
    }
    private AddParticipantResponse addServiceUser(Long userId, Long scheduleId, Long memberId) {
        Member requester = memberRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member addressee = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatusIn(scheduleId, memberId, List.of(INVITED,ACCEPTED))) throw new BusinessException(ErrorCode.REINVITE_NOT_ALLOWED);

        ScheduleParticipant sp = ScheduleParticipant.ofMember(s,addressee, requester);
        s.addParticipant(sp);
        scheduleParticipantRepository.save(sp);
        scheduleParticipantRepository.flush();

        Notification notification = Notification.builder()
                .member(addressee)
                .actor(requester)
                .type(NotificationType.INVITED_TO_SCHEDULE)
                .resourceId(sp.getId())
                .payloadJson(
                        Map.of(
                                "scheduleId", s.getId(),
                                "scheduleTitle", s.getTitle(),
                                "scheduleStartAt", s.getStartAt(),
                                "scheduleEndAt", s.getEndAt(),
                                "actorName", requester.getName()
                        )
                )
                .dedupeKey("scheduleInvite:" + sp.getId())
                .build();

        notificationRepository.save(notification);

        return toAddParticipantResponse(sp);
    }
    private AddParticipantResponse addName(Long userId, Long scheduleId, String name) {
        String normalized = name == null? null: name.trim();
        if(normalized == null || normalized.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);

        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        ScheduleParticipant sp = ScheduleParticipant.ofName(s,name, user);
        s.addParticipant(sp);


        Map<String, Object> newParticipant = new HashMap<>();
        newParticipant.put("newParticipantName", sp.getName());

        CalendarHistory history = CalendarHistory.builder()
                .calendar(s.getCalendar())
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_PARTICIPANT_ADDED)
                .changedFields(newParticipant)
                .build();
        calendarHistoryRepository.save(history);

        embeddingEnqueueService.enqueueAfterCommit(s.getId());

        return toAddParticipantResponse(sp);
    }

    private AddParticipantResponse toAddParticipantResponse(ScheduleParticipant sp) {
        return new AddParticipantResponse(
                sp.getId(),
                sp.getSchedule().getId(),
                sp.getMember() != null ? sp.getMember().getId() : null,
                sp.getName(),
                sp.getStatus(),
                sp.getInvitedAt()
        );
    }

    private ScheduleParticipantResponse toResponse(ScheduleParticipant sp) {
        Long memberId = (sp.getMember() != null) ? sp.getMember().getId() : null;
        return new ScheduleParticipantResponse(
                sp.getId(),
                sp.getSchedule().getId(),
                memberId,
                sp.getName(),
                sp.getInvitedAt(),
                sp.getRespondedAt(),
                sp.getStatus()
        );
    }

}
