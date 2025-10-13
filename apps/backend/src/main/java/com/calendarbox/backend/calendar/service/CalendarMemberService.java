package com.calendarbox.backend.calendar.service;


import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.calendar.dto.request.CalendarInvitedRespondRequest;
import com.calendarbox.backend.calendar.dto.request.InviteMembersRequest;
import com.calendarbox.backend.calendar.dto.response.CalendarInviteRespondResponse;
import com.calendarbox.backend.calendar.dto.response.InviteMembersResponse;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.enums.NotificationType;
import com.calendarbox.backend.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CalendarMemberService {
    private final CalendarMemberRepository calendarMemberRepository;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final CalendarHistoryRepository calendarHistoryRepository;

    public InviteMembersResponse inviteMembers(Long inviterId, Long calendarId, InviteMembersRequest request){
        Member inviter = memberRepository.findById(inviterId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> memberIds = Optional.ofNullable(request)
                .map(InviteMembersRequest::members)
                .orElse(Collections.emptyList());
        if (memberIds.isEmpty()){
            throw new BusinessException(ErrorCode.REQUEST_NO_CHANGES);
        }

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        if (calendar.getType() != CalendarType.GROUP){
            throw new BusinessException(ErrorCode.INVITE_ONLY_FOR_GROUP);
        }

        boolean inviterIsMember = calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(
                calendarId, inviterId, CalendarMemberStatus.ACCEPTED);
        if (!inviterIsMember){
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        Set<Long> orderedUnique = new LinkedHashSet<>(memberIds);
        List<Long> targets = new ArrayList<>(orderedUnique);

        List<Long> successIds = new ArrayList<>();
        List<Long> failureIds = new ArrayList<>();

        targets.removeIf(id -> {
            if (Objects.equals(id, inviterId)) {
                failureIds.add(id);
                return true;
            }
            return false;
        });
        if (targets.isEmpty()){
            return new InviteMembersResponse(calendarId, 0, failureIds.size(), successIds, failureIds);
        }

        List<Member> foundMembers = memberRepository.findAllById(targets);
        Map<Long, Member> foundMap = foundMembers.stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        for (Long id : targets) {
            if (!foundMap.containsKey(id)) {
                failureIds.add(id);
            }
        }

        List<Long> validTargetIds = targets.stream().filter(foundMap::containsKey).toList();
        if (validTargetIds.isEmpty()){
            return new InviteMembersResponse(calendarId, 0, failureIds.size(), successIds, failureIds);
        }

        List<CalendarMember> existing = calendarMemberRepository
                .findByCalendarIdAndMemberIds(calendarId, validTargetIds);

        Map<Long, CalendarMember> existingByMemberId = existing.stream()
                .collect(Collectors.toMap(cm -> cm.getMember().getId(), cm -> cm));

        List<CalendarMember> toInsert = new ArrayList<>();

        for (Long targetId : validTargetIds){
            CalendarMember existed = existingByMemberId.get(targetId);

            if (existed == null){
                Member target = foundMap.get(targetId);
                toInsert.add(CalendarMember.invite(calendar, target)); // status=INVITED
                successIds.add(targetId);
                continue;
            }

            switch (existed.getStatus()){
                case REJECTED -> {
                    existed.reinvite();
                    successIds.add(targetId);
                }
                case INVITED, ACCEPTED -> {
                    failureIds.add(targetId);
                }
            }
        }

        if (!toInsert.isEmpty()){
            calendarMemberRepository.saveAll(toInsert);
        }

        List<Notification> notifications = new ArrayList<>();
        for(Long targetId:successIds){
            Member addressee = foundMap.get(targetId);
            CalendarMember cm = existingByMemberId.get(targetId);
            if (cm == null) {
                cm = toInsert.stream()
                        .filter(c -> c.getMember().getId().equals(targetId))
                        .findFirst()
                        .orElseThrow();
            }
            Notification notification = Notification.builder()
                    .member(addressee)
                    .actor(inviter)
                    .type(NotificationType.INVITED_TO_CALENDAR)
                    .resourceId(cm.getId())
                    .payloadJson(
                            toJson(Map.of(
                                    "calendarId", calendar.getId(),
                                    "calendarName", calendar.getName(),
                                    "actorName", inviter.getName()
                            ))
                    )
                    .dedupeKey("calendarInvite:" + cm.getId())
                    .build();

            notifications.add(notification);
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }

        return new InviteMembersResponse(
                calendarId,
                successIds.size(),
                failureIds.size(),
                successIds,
                failureIds
        );
    }

    public CalendarInviteRespondResponse respond(Long responderId, Long calendarMemberId, CalendarInvitedRespondRequest request){
        CalendarMember calendarMember = calendarMemberRepository.findById(calendarMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_MEMBER_NOT_FOUND));

        if(!calendarMember.getMember().getId().equals(responderId)){
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        if(calendarMember.getStatus() != CalendarMemberStatus.INVITED){
            throw new BusinessException(ErrorCode.CALENDAR_MEMBER_ALREADY_RESPONDED);
        }

        switch(request.action()){
            case ACCEPT -> {
                calendarMember.accept();
                calendarHistoryRepository.save(
                        CalendarHistory.builder()
                        .calendar(calendarMember.getCalendar())
                        .actor(calendarMember.getMember())
                        .entityId(calendarMember.getCalendar().getId())
                        .type(CalendarHistoryType.CALENDAR_MEMBER_ADDED)
                        .changedFields(toJson(Map.of("newCalendarMemberName: ", calendarMember.getMember().getName())))
                        .build()
                );
            }
            case REJECT -> calendarMember.reject();
        }

        return new CalendarInviteRespondResponse(
                calendarMember.getId(),
                calendarMember.getCalendar().getId(),
                calendarMember.getMember().getId(),
                calendarMember.getStatus(),
                calendarMember.getRespondedAt()
        );
    }

    public String deleteCalendarMember(Long userId, Long calendarMemberId){
        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        CalendarMember calendarMember = calendarMemberRepository.findById(calendarMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_MEMBER_NOT_FOUND));

        Long targetId = calendarMember.getMember().getId();

        boolean isWithdraw = false;
        if(user.getId().equals(targetId)){
            calendarMemberRepository.delete(calendarMember);
            isWithdraw = true;
        } else {
            if(!calendarMember.getCalendar().getOwner().getId().equals(user.getId()))
                throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
            calendarMemberRepository.delete(calendarMember);
        }

        calendarHistoryRepository.save(
                CalendarHistory.builder()
                        .calendar(calendarMember.getCalendar())
                        .actor(user)
                        .entityId(calendarMember.getCalendar().getId())
                        .type(CalendarHistoryType.CALENDAR_MEMBER_REMOVED)
                        .changedFields(toJson(Map.of("removedCalendarMemberName: ", calendarMember.getMember().getName())))
                        .build()
        );

        String msg = isWithdraw?
                calendarMember.getCalendar().getName() + " 캘린더에서 탈퇴했습니다.":
                calendarMember.getCalendar().getName() + " 캘린더에서 " + calendarMember.getMember().getName() + "님을 추방시켰습니다.";
        return msg;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
}
