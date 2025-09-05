package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
