package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.place.repository.PlaceRepository;
import com.calendarbox.backend.schedule.domain.SchedulePlace;
import com.calendarbox.backend.schedule.dto.response.SchedulePlaceDto;
import com.calendarbox.backend.schedule.repository.SchedulePlaceRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulePlaceQueryService {

    private final SchedulePlaceRepository schedulePlaceRepository;

    public SchedulePlaceDto getDetail(Long userId, Long scheduleId, Long schedulePlaceId) {
        //권한 체크
        SchedulePlace sp =  schedulePlaceRepository.findById(schedulePlaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PLACE_NOT_FOUND));

        Place p = sp.getPlace();
        return toDto(sp,p);
    }
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
}
