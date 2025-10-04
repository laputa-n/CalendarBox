package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.dto.response.CalendarDetail;
import com.calendarbox.backend.calendar.dto.response.CalendarListItem;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {

    private final CalendarMemberRepository calendarMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final CalendarRepository calendarRepository;

    public Page<CalendarListItem> listCalendars(
            Long viewerId,
            Long memberId,                 // null -> me
            CalendarType type,
            CalendarMemberStatus status,
            Visibility visibility,
            Pageable pageable
    ) {
        Long targetId = (memberId == null) ? viewerId : memberId;
        boolean selfView = viewerId.equals(targetId);

        Pageable fixed = fixSort(pageable); // name ASC, id DESC

        if (selfView) {
            return calendarMemberRepository.findSelf(targetId, status,type, visibility, fixed);
        }

        boolean friends = friendshipRepository.existsAcceptedBetween(viewerId, targetId);
        if (!friends) {
            throw new BusinessException(ErrorCode.FRIENDSHIP_REQUIRED);
        }

        return calendarMemberRepository.findFriendVisible(targetId, type, fixed);
    }

    public CalendarDetail getCalendarDetail(Long userId, Long calendarId){
        Optional<Calendar> c = calendarRepository.findById(calendarId);
        if (c.isEmpty()) {
            throw new BusinessException(ErrorCode.CALENDAR_NOT_FOUND);
        }
        Calendar cal = c.get();
        if(!calendarMemberRepository.existsByMember_IdAndCalendar_Id(userId, calendarId)){
            throw new BusinessException(ErrorCode.CALENDAR_NOT_FOUND);
        }
        CalendarType ct = cal.getType();

        CalendarDetail cd = ct.equals(CalendarType.GROUP)? new CalendarDetail(cal.getId(), cal.getName(),cal.getType(), cal.getVisibility(), calendarMemberRepository.countCalendarMembersByCalendarId(calendarId), cal.getCreatedAt(),cal.getUpdatedAt()): new CalendarDetail(cal.getId(), cal.getName(),cal.getType(), cal.getVisibility(),null, cal.getCreatedAt(),cal.getUpdatedAt());
        return cd;
    }
    private Pageable fixSort(Pageable p) {
        int page = (p == null) ? 0 : p.getPageNumber();
        int size = (p == null) ? 10 : p.getPageSize();
        return PageRequest.of(page, size);
    }
}
