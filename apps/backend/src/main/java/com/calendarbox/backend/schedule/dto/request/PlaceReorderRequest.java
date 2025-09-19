package com.calendarbox.backend.schedule.dto.request;

import java.util.List;

public record PlaceReorderRequest(List<PlaceReorderItem> positions) {
}
