package com.calendarbox.backend.schedule.dto.response;

import java.time.LocalDate;

public record RecurrenceExceptionResponse(Long exceptionId, LocalDate exceptionDate) {}
