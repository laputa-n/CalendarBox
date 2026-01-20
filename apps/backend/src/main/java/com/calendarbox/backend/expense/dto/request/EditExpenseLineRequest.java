package com.calendarbox.backend.expense.dto.request;

import io.micrometer.common.lang.Nullable;

public record EditExpenseLineRequest(
        @Nullable String label,
        @Nullable Integer quantity,
        @Nullable Long unitAmount
) {
}
