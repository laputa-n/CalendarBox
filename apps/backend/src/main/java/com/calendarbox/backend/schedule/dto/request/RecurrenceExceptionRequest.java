package com.calendarbox.backend.schedule.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RecurrenceExceptionRequest(@NotNull LocalDate exceptionDate) {}
