package com.calendarbox.backend.calendar.dto.response;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarListItem(
        Long calendarId,
        String name,
        CalendarType type,
        CalendarMemberStatus status,
        Visibility visibility,
        Boolean isDefault
) { }
