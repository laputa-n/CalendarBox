package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.calendar.dto.request.CalendarEditRequest;
import com.calendarbox.backend.calendar.dto.response.CalendarEditResponse;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final CalendarMemberRepository calendarMemberRepository;

    @Transactional
    public Calendar create(Long creatorId, String name, CalendarType type, Visibility visibility, boolean isDefault){
        Member creator = memberRepository.findByIdForUpdate(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if(calendarRepository.existsByOwner_IdAndName(creatorId,name)){
            throw new BusinessException(ErrorCode.CALENDAR_NAME_DUPLICATE);
        }
        Calendar calendar = Calendar.create(creator, name, type, visibility);
        calendarRepository.save(calendar);

        boolean makeDefault = isDefault || !calendarMemberRepository.existsByMember_IdAndIsDefaultTrue(creatorId);
        if (makeDefault) {
            calendarMemberRepository.unsetDefaultForMember(creatorId);
        }

        CalendarMember calendarMember = CalendarMember.create(calendar,creator,makeDefault);
        calendarMemberRepository.save(calendarMember);

        return calendar;
    }

    @Transactional
    public CalendarEditResponse editCalendar(Long userId, Long calendarId, CalendarEditRequest request){
        Calendar c = calendarRepository.findByIdAndOwner_Id(calendarId,userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        if (request.name() == null && request.visibility() == null) {
            throw new BusinessException(ErrorCode.REQUEST_NO_CHANGES);
        }

        if(request.name() != null){
            String newName = request.name();
            if(!newName.equals(c.getName()) && calendarRepository.existsByOwner_IdAndName(userId,newName)){
                throw new BusinessException(ErrorCode.CALENDAR_NAME_DUPLICATE);
            }
            c.rename(request.name());
        }

        if(request.visibility() != null){
            c.changeVisibility(request.visibility());
        }


        return new CalendarEditResponse(
                c.getId(),
                c.getName(),
                c.getVisibility(),
                c.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteCalendar(Long userId, Long calendarId){
        Calendar calendar = calendarRepository.findByIdAndOwner_Id(calendarId,userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        boolean wasDefault = calendarMemberRepository.existsByCalendar_IdAndMember_IdAndIsDefaultTrue(calendarId,userId);

        calendarMemberRepository.deleteByCalendarId(calendarId);

        calendarRepository.delete(calendar);

        if (wasDefault && !calendarMemberRepository.existsByMember_IdAndIsDefaultTrue(userId)) {
            List<CalendarMember> cand = calendarMemberRepository
                    .findDefaultCandidate(userId, PageRequest.of(0, 1));
            if (!cand.isEmpty()) {
                calendarMemberRepository.unsetDefaultForMember(userId);
                cand.get(0).makeDefault();
            }
        }
    }
}
