package com.calendarbox.backend.schedule.dto.response;

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
}
