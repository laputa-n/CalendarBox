package com.calendarbox.backend.calendar.dto.request;

import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCalendarRequest(

        @NotBlank(message = "{calendar.name.notblank}")
        @Size(max = 100, message = "{calendar.name.size}")
        String name,
        CalendarType type,
        Visibility visibility,
        boolean isDefault
        ) { }
