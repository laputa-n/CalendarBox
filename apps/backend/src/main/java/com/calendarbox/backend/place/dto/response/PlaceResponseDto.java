package com.calendarbox.backend.place.dto.response;

import com.calendarbox.backend.place.domain.Place;

public record PlaceResponseDto(
        Long id,
        String title,
        String link,
        String category,
        String description,
        String address,
        String roadAddress,
        Double lng,
        Double lat,
        String provider,
        String providerKey
) {
    public static PlaceResponseDto from(Place place){
        return new PlaceResponseDto(
                place.getId(),
                place.getTitle(),
                place.getLink(),
                place.getCategory(),
                place.getDescription(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLng().doubleValue(),
                place.getLat().doubleValue(),
                place.getProvider(),
                place.getProviderPlaceKey()
        );
    }
}
