package com.calendarbox.backend.schedule.dto.request;

import java.util.List;

public record TodoReorderRequest(List<TodoReorderItem> orders) {
}
