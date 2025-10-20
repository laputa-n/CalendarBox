package com.calendarbox.backend.schedule.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

public record SchedulePlaceEditRequest(
        @Size(min = 1,max = 50, message = "{place.name.size}")
        String name
) { }
