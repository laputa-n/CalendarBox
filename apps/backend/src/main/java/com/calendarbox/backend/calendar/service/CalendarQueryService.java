package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarMember;
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
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {

    private final CalendarMemberRepository calendarMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;

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

    @Transactional(readOnly = true)
    public CalendarDetail getCalendarDetail(Long viewerId, Long calendarId) {
        Calendar cal = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        Long ownerId = cal.getOwner().getId();

        if (Objects.equals(ownerId, viewerId)) {
            return toDetail(cal);
        }

        boolean isGroup = cal.getType() == CalendarType.GROUP;

        boolean isGroupMember = isGroup && calendarMemberRepository
                .existsByCalendar_IdAndMember_IdAndStatusIn(
                        calendarId, viewerId,
                        List.of(
                                CalendarMemberStatus.ACCEPTED,
                                CalendarMemberStatus.INVITED
                        )
                );

        switch (cal.getType()) {
            case PERSONAL -> {
                return switch (cal.getVisibility()) {
                    case PUBLIC -> toDetail(cal);
                    case PROTECTED -> {
                        boolean friendWithOwner = friendshipRepository.existsAcceptedBetween(viewerId, ownerId);
                        if (friendWithOwner) yield toDetail(cal);
                        throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
                    }
                    case PRIVATE -> {
                        throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
                    }
                };
            }
            case GROUP -> {
                return switch (cal.getVisibility()) {
                    case PUBLIC -> toDetail(cal);
                    case PROTECTED -> {
                        if (isGroupMember || isFriendWithAnyAcceptedMember(viewerId, calendarId)) {
                            yield toDetail(cal);
                        }
                        throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
                    }
                    case PRIVATE -> {
                        if (isGroupMember) yield toDetail(cal);
                        throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
                    }
                };
            }
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private boolean isFriendWithAnyAcceptedMember(Long viewerId, Long calendarId) {
        return calendarMemberRepository.existsFriendshipWithAnyAcceptedMember(calendarId, viewerId);
    }
    private CalendarDetail toDetail(Calendar c){
        return (c.getType() == CalendarType.GROUP)
                ? new CalendarDetail(
                c.getId(), c.getName(), c.getType(), c.getVisibility(),
                calendarMemberRepository.countCalendarMembersByCalendarId(c.getId()),
                c.getCreatedAt(), c.getUpdatedAt()
        )
                : new CalendarDetail(
                c.getId(), c.getName(), c.getType(), c.getVisibility(),
                null,
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
    private Pageable fixSort(Pageable p) {
        int page = (p == null) ? 0 : p.getPageNumber();
        int size = (p == null) ? 10 : p.getPageSize();
        return PageRequest.of(page, size);
    }
}
