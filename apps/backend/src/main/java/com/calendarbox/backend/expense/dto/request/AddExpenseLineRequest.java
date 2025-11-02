package com.calendarbox.backend.expense.dto.request;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddExpenseLineRequest(
        @NotBlank String label,
        @Nullable Integer quantity,
        @Nullable Long unitAmount,
        @NotNull Long lineAmount
        ) {
    public int resolvedQuantity() {
        return (quantity == null || quantity <= 0) ? 1 : quantity;
    }

    public long resolvedUnitAmount() {
        return (unitAmount != null) ? unitAmount : lineAmount / resolvedQuantity();
    }
}
