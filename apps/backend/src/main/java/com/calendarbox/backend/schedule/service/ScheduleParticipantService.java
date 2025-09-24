package com.calendarbox.backend.schedule.service;


import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
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
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

import static com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus.INVITED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class ScheduleParticipantService {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final MemberRepository memberRepository;
    private final FriendshipRepository friendshipRepository;
    private final ScheduleRepository scheduleRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    public AddParticipantResponse add(Long userId, Long scheduleId, AddParticipantRequest request) {

        return switch(request.mode()){
            case FRIEND -> addFriend(userId, scheduleId, request.memberId());
            case NAME -> addName(userId,scheduleId,request.name());
        };
    }

    public void remove(Long userId, Long scheduleId, Long participantId) {
        //권한 체크
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar c = s.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(c.getId(),userId, CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        ScheduleParticipant sp = scheduleParticipantRepository.findById(participantId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));

        s.removeParticipant(sp);
    }

    public ScheduleParticipantResponse respond(Long userId, Long scheduleId, Long participantId, ParticipantRespondRequest request){
        ScheduleParticipant sp = scheduleParticipantRepository.findById(participantId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));
        if(!Objects.equals(sp.getMember().getId(), userId)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if(sp.getStatus() != INVITED) return toResponse(sp);

        switch(request.action()){
            case ACCEPT -> sp.accept();
            case REJECT -> sp.decline();
        }
        return toResponse(sp);
    }
    private AddParticipantResponse addFriend(Long userId, Long scheduleId, Long memberId) {
        Member requester = memberRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member addressee = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!friendshipRepository.existsAcceptedBetween(requester.getId(), addressee.getId())) throw new BusinessException(ErrorCode.FRIENDSHIP_REQUIRED);
        if (scheduleParticipantRepository.existsBySchedule_IdAndMember_Id(scheduleId, memberId)) throw new BusinessException(ErrorCode.REINVITE_NOT_ALLOWED);

        ScheduleParticipant sp = ScheduleParticipant.ofMember(s,addressee);
        s.addParticipant(sp);
        scheduleParticipantRepository.save(sp);

        Notification notification = Notification.builder()
                .member(addressee)
                .actor(requester)
                .type(NotificationType.INVITED_TO_SCHEDULE)
                .resourceId(sp.getId())
                .payloadJson(
                        toJson(Map.of(
                                "scheduleId", s.getId(),
                                "scheduleTitle", s.getTitle(),
                                "scheduleStartAt", s.getStartAt(),
                                "scheduleEndAt", s.getEndAt(),
                                "actorName", requester.getName()
                        ))
                )
                .dedupeKey("scheduleInvite:" + sp.getId())
                .build();

        notificationRepository.save(notification);

        return toAddParticipantResponse(sp);
    }
    private AddParticipantResponse addName(Long userId, Long scheduleId, String name) {
        String normalized = name == null? null: name.trim();
        if(normalized == null || normalized.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);

        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        ScheduleParticipant sp = ScheduleParticipant.ofName(s,name);
        s.addParticipant(sp);

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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
}
