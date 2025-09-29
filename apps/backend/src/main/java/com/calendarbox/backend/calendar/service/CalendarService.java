package com.calendarbox.backend.calendar.service;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.calendar.dto.request.CalendarEditRequest;
import com.calendarbox.backend.calendar.dto.response.CalendarEditResponse;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final ObjectMapper objectMapper;
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
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Calendar c = calendarRepository.findByIdAndOwner_Id(calendarId,userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_NOT_FOUND));

        if (request.name() == null && request.visibility() == null) {
            throw new BusinessException(ErrorCode.REQUEST_NO_CHANGES);
        }
        Map<String,Object> diff = new LinkedHashMap<>();

        if(request.name() != null){
            String oldName = c.getName();
            String newName = request.name();
            if(!newName.equals(c.getName()) && calendarRepository.existsByOwner_IdAndName(userId,newName)){
                throw new BusinessException(ErrorCode.CALENDAR_NAME_DUPLICATE);
            }
            c.rename(request.name());
            diff.put("name", Map.of("old",oldName,"new",newName));
        }

        if(request.visibility() != null){
            Visibility oldVisibility = c.getVisibility();
            c.changeVisibility(request.visibility());
            diff.put("visibility",Map.of("old",oldVisibility,"new",request.visibility()));
        }

        CalendarHistory history = CalendarHistory.builder()
                .calendar(c)
                .actor(user)
                .entityId(c.getId())
                .type(CalendarHistoryType.CALENDAR_UPDATED)
                .changedFields(toJson(diff))
                .build();
        calendarHistoryRepository.save(history);


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
    private String toJson(Object value){
        try{
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
}
