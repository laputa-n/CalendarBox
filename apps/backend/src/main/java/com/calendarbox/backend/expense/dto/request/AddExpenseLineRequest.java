package com.calendarbox.backend.expense.dto.request;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;

public record AddExpenseLineRequest(
        @NotBlank String label,
        @NotNull Integer quantity,
        @NotNull Long unitAmount
        ) {
}
