package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.dto.response.CalendarMemberItem;
import com.calendarbox.backend.calendar.enums.CalendarMemberSort;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CalendarMemberQueryService {

    private final CalendarMemberRepository calendarMemberRepository;

    public Page<CalendarMemberItem> listMembers(Long viewerId, Long calendarId, CalendarMemberStatus status, CalendarMemberSort sort, Pageable pageable){
        boolean canView = calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(
                calendarId, viewerId, CalendarMemberStatus.ACCEPTED
        );
        if(!canView){
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        Pageable p = PageRequest.of(pageable.getPageNumber(),pageable.getPageSize());

        return switch(sort){
            case NAME_ASC -> calendarMemberRepository.findMembersOrderByNameAsc(calendarId,status,p);
            case NAME_DESC -> calendarMemberRepository.findMembersOrderByNameDesc(calendarId,status,p);
            case CREATED_ASC -> calendarMemberRepository.findMembersOrderByCreatedAtAsc(calendarId,status,p);
            case CREATED_DESC -> calendarMemberRepository.findMembersOrderByCreatedAtDesc(calendarId,status,p);
        };
    }
}
