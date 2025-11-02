package com.calendarbox.backend.expense.dto.response;

import com.calendarbox.backend.expense.enums.ExpenseSource;

import java.time.Instant;
import java.time.LocalDate;

public record ExpenseListItem(
        Long expenseId, String name, Long amount, Instant paidAt, LocalDate occurrenceDate, ExpenseSource source
) {
}
