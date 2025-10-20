package com.calendarbox.backend.place.dto.response;


public record PlacePreview(
        String provider, String providerPlaceKey,
        String title, String link, String category, String description,
        String address, String roadAddress, Double lat, Double lng
) {}
