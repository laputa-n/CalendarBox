package com.calendarbox.backend.expense.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDate;

public record AddExpenseRequest(
        @NotBlank String name,
        @NotNull @Positive Long amount,
        @Nullable Instant paidAt,
        @Nullable LocalDate occurrenceDate) {
}
