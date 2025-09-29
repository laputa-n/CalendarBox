package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.place.repository.PlaceRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.SchedulePlace;
import com.calendarbox.backend.schedule.dto.request.AddSchedulePlaceRequest;
import com.calendarbox.backend.schedule.dto.request.PlaceReorderRequest;
import com.calendarbox.backend.schedule.dto.request.SchedulePlaceEditRequest;
import com.calendarbox.backend.schedule.dto.response.SchedulePlaceDto;
import com.calendarbox.backend.schedule.repository.SchedulePlaceRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = false)
@RequiredArgsConstructor
public class SchedulePlaceService {
    private final SchedulePlaceRepository schedulePlaceRepository;
    private final PlaceRepository placeRepository;
    private final ScheduleRepository scheduleRepository;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;

    public SchedulePlaceDto addPlace(Long userId, Long scheduleId, AddSchedulePlaceRequest req){
        //user 검증 필요
        int nextPos = schedulePlaceRepository.findMaxPositionByScheduleId(scheduleId)+1;

        return switch(req.mode()){
            case MANUAL -> handleManual(userId,scheduleId,req,nextPos);
            case EXISTING -> handleExisting(userId,scheduleId,req,nextPos);
            case PROVIDER -> handleProvider(userId,scheduleId,req,nextPos);
        };
    }

    public void delete(Long userId, Long scheduleId, Long schedulePlaceId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        SchedulePlace sp = schedulePlaceRepository.findById(schedulePlaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PLACE_NOT_FOUND));
        schedulePlaceRepository.delete(sp);
        Schedule s = sp.getSchedule();

        CalendarHistory history = CalendarHistory.builder()
                .calendar(s.getCalendar())
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_LOCATION_REMOVED)
                .changedFields("placeName: " + sp.getName())
                .build();
        calendarHistoryRepository.save(history);
    }

    public SchedulePlaceDto edit(Long userId, Long scheduleId, Long schedulePlaceId, SchedulePlaceEditRequest req){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String newName = normalize(req.name());
        if(schedulePlaceRepository.existsByScheduleIdAndName(scheduleId, newName)) throw new BusinessException(ErrorCode.SCHEDULE_PLACE_NAME_DUP);

        SchedulePlace sp = schedulePlaceRepository.findById(schedulePlaceId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PLACE_NOT_FOUND));
        String oldName = sp.getName();
        sp.changeName(newName);
        Schedule s = sp.getSchedule();

        Map<String,Object> diff = new HashMap<>();
        diff.put("oldName", oldName);
        diff.put("newName", newName);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(s.getCalendar())
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_LOCATION_UPDATED)
                .changedFields(toJson(diff))
                .build();
        calendarHistoryRepository.save(history);
        return toDto(sp,sp.getPlace());
    }

    public List<SchedulePlaceDto> reorder(Long userId, Long scheduleId, PlaceReorderRequest req){
        if(req.positions() == null || req.positions().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_JSON);
        }

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Map<Long,Integer> desired = new HashMap<>();
        for(var it: req.positions()){
            if(it.position() < 0) throw new BusinessException(ErrorCode.INVALID_JSON);
            desired.put(it.schedulePlaceId(), it.position());
        }

        var schedulePlaces = schedulePlaceRepository.findAllByIds(desired.keySet());

        for(var sp:schedulePlaces){
            if(!sp.getSchedule().getId().equals(scheduleId)) throw new BusinessException(ErrorCode.SCHEDULE_PLACE_NOT_MATCH);

            Integer newPos = desired.get(sp.getId());

            if(newPos != null && sp.getPosition() != newPos) sp.changePosition(newPos);
        }

        List<SchedulePlaceDto> list = new ArrayList<>();
        for(var sp:schedulePlaceRepository.findAllByScheduleId(scheduleId)){
            SchedulePlaceDto dto =toSimpleDto(sp);
            list.add(dto);
        }

        return list;
    }
    private SchedulePlaceDto handleProvider(Long userId, Long scheduleId, AddSchedulePlaceRequest req, int nextPos) {
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Place p = placeRepository.findByProviderAndProviderPlaceKey(req.provider(), req.providerPlaceKey())
                .orElseGet(() -> {
                    Place np = Place.builder()
                            .provider(req.provider())
                            .providerPlaceKey(req.providerPlaceKey())
                            .title(req.title())
                            .category(req.category())
                            .description(req.description())
                            .address(req.address())
                            .roadAddress(req.roadAddress())
                            .link(req.link())
                            .lat(BigDecimal.valueOf(req.lat()))
                            .lng(BigDecimal.valueOf(req.lng()))
                            .build();
                    return placeRepository.save(np);
                });
        if(schedulePlaceRepository.existsByScheduleIdAndPlaceId(scheduleId, p.getId())) {
            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_DUP);
        }
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(()->new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        String n = hasText(req.name()) ? req.name().trim() : req.title();
        n = normalize(n);
        SchedulePlace sp = SchedulePlace.builder()
                .schedule(s)
                .place(p)
                .name(n)
                .position(nextPos)
                .build();
        schedulePlaceRepository.save(sp);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(s.getCalendar())
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_LOCATION_ADDED)
                .changedFields("placeName: " + n)
                .build();
        calendarHistoryRepository.save(history);
        return toDto(sp,p);
    }
    private SchedulePlaceDto handleExisting(Long userId, Long scheduleId, AddSchedulePlaceRequest req, int nextPos) {
        if(req.placeId() == null) throw new BusinessException(ErrorCode.EXISTING_PLACE_ID_NEED);

        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Place p = placeRepository.findById(req.placeId()).orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        if(schedulePlaceRepository.existsByScheduleIdAndPlaceId(scheduleId, p.getId())) {
            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_DUP);
        }

        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(()->new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        String n = hasText(req.name()) ? req.name().trim() : req.title();
        n = normalize(n);
        SchedulePlace sp = SchedulePlace.builder()
                .schedule(s)
                .place(p)
                .name(n)
                .position(nextPos)
                .build();
        schedulePlaceRepository.save(sp);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(s.getCalendar())
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_LOCATION_ADDED)
                .changedFields("placeName: " + n)
                .build();
        calendarHistoryRepository.save(history);

        return toDto(sp,p);
    }

    private SchedulePlaceDto handleManual(Long userId, Long scheduleId, AddSchedulePlaceRequest request, int nextPos) {
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String normalized = normalize(request.name());
        if(normalized == null || normalized.isBlank()){
            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_NAME_NEED);
        }

        if(schedulePlaceRepository.existsByScheduleIdAndName(scheduleId, normalized)){
            throw new BusinessException(ErrorCode.SCHEDULE_PLACE_NAME_DUP);
        }
        Schedule s = scheduleRepository.findById(scheduleId).orElseThrow(()->new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        SchedulePlace sp = SchedulePlace.builder()
                .schedule(s)
                .name(normalized)
                .position(nextPos)
                .build();
        schedulePlaceRepository.save(sp);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(s.getCalendar())
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_LOCATION_ADDED)
                .changedFields("placeName: " + normalized)
                .build();
        calendarHistoryRepository.save(history);

        return toDto(sp,null);
    }

    private static String normalize(String s) {return s == null ? null : s.trim();}
    private static boolean hasText(String s){ return s != null && !s.trim().isEmpty(); }

    private SchedulePlaceDto toDto(SchedulePlace sp, Place p) {
        Long placeId = (sp.getPlace() != null) ? sp.getPlace().getId() : null;
        SchedulePlaceDto.PlaceSnapShot snapshot = (p == null) ? null :
                new SchedulePlaceDto.PlaceSnapShot(
                        p.getId(), p.getTitle(), p.getCategory(), p.getDescription(), p.getAddress(), p.getRoadAddress(),
                        p.getLink(), p.getLat(), p.getLng(), p.getProvider(), p.getProviderPlaceKey()
                );

        return new SchedulePlaceDto(
                sp.getId(),
                sp.getSchedule().getId(),
                placeId,
                sp.getName(),
                sp.getPosition(),
                sp.getCreatedAt(),
                snapshot
        );
    }

    private SchedulePlaceDto toSimpleDto(SchedulePlace sp) {
        return new SchedulePlaceDto(
                sp.getId(),
                null,
                null,
                sp.getName(),
                sp.getPosition(),
                null,
                null
        );
    }

    private String toJson(Object value){
        try{
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e){
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,"알림 페이로드 직렬화 실패");

        }
    }
}
