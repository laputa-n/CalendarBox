package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.dto.response.CalendarHistoryDto;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarHistoryQueryService {
    private final CalendarMemberRepository calendarMemberRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    public Page<CalendarHistoryDto> getHistories(Long userId, Long calendarId, Instant from, Instant to) {
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendarId,userId, CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        Pageable pageable = PageRequest.of(0,20);
        return calendarHistoryRepository.findAllByCalendar_Id(calendarId,from,to,pageable).map(
                h -> new CalendarHistoryDto(
                        h.getId(),
                        calendarId,
                        (h.getActor() != null ? h.getActor().getId() : null),
                        h.getEntityId(),
                        h.getType(),
                        h.getChangedFields(),
                        h.getCreatedAt()
                ));

    }
}
