package com.calendarbox.backend.place.dto.response;

import java.util.List;

public record NaverLocalResponse(List<Item> items, int total) {
    public record Item(
            String title,
            String link,
            String category,
            String description,
            String address,
            String roadAddress,
            String mapx,
            String mapy
    ) {}
}
