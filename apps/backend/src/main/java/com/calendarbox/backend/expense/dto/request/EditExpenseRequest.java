package com.calendarbox.backend.expense.dto.request;

import java.time.Instant;

public record EditExpenseRequest(
        String name,
        Long amount,
        Instant paidAt
) {
}
