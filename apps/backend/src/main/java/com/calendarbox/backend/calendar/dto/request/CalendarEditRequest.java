package com.calendarbox.backend.calendar.dto.request;

import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;

public record CalendarEditRequest(
        @Size(max = 100, message = "{calendar.name.size}")
        String name,
        Visibility visibility,
        CalendarType type
) {
}
