package com.calendarbox.backend.expense.dto.response;

import com.calendarbox.backend.expense.enums.ExpenseSource;

import java.time.Instant;
import java.time.LocalDate;

public record AddExpenseResponse(
        Long expenseId,
        Long scheduleId,
        String name,
        Long amount,
        Instant paidAt,
        LocalDate occurrenceDate,
        Instant createdAt,
        ExpenseSource source
) {
}
