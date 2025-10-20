package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.PlaceEnrollMode;
import jakarta.validation.constraints.NotNull;

public record AddSchedulePlaceRequest(
        @NotNull PlaceEnrollMode mode,

        // MANUAL
        String name,

        // EXISTING
        Long placeId,

        // PROVIDER
        String provider,           // e.g. "NAVER"
        String providerPlaceKey,   // e.g. "n_123456"
        String title,
        String category,
        String description,
        String address,
        String roadAddress,
        String link,
        Double lat,
        Double lng
) {}
