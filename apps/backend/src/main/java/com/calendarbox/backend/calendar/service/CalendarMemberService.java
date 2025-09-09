package com.calendarbox.backend.calendar.service;


import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.calendar.dto.request.CalendarInvitedRespondRequest;
import com.calendarbox.backend.calendar.dto.request.InviteMembersRequest;
import com.calendarbox.backend.calendar.dto.response.CalendarInviteRespondResponse;
import com.calendarbox.backend.calendar.dto.response.InviteMembersResponse;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
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

    public InviteMembersResponse inviteMembers(Long inviterId, Long calendarId, InviteMembersRequest request){
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
            case ACCEPT -> calendarMember.accept();
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
}
