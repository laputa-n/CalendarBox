package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.schedule.domain.SchedulePlace;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SchedulePlaceDto(
        Long schedulePlaceId,
        Long scheduleId,
        Long placeId,
        String name, // 외부에서 넣을 때 입력 안할 시에 스냅샷의 title로
        Integer position,
        Instant createdAt,
        PlaceSnapShot place
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlaceSnapShot(
            Long id,
            String title,
            String category,
            String description,
            String address,
            String roadAddress,
            String link,
            BigDecimal lat,
            BigDecimal lng,
            String provider,
            String providerPlaceKey

    ){}

    public static SchedulePlaceDto of(SchedulePlace sp) {
        Place p = sp.getPlace();
        Long placeId = (p != null) ? p.getId() : null;

        PlaceSnapShot snapshot = (p == null) ? null :
                new PlaceSnapShot(
                        p.getId(),
                        p.getTitle(),
                        p.getCategory(),
                        p.getDescription(),
                        p.getAddress(),
                        p.getRoadAddress(),
                        p.getLink(),
                        p.getLat(),
                        p.getLng(),
                        p.getProvider(),
                        p.getProviderPlaceKey()
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
