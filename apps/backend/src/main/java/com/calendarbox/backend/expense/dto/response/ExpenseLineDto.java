package com.calendarbox.backend.expense.dto.response;

import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.domain.ExpenseLine;

import java.time.Instant;

public record ExpenseLineDto(
        Long expenseLineId,
        Long expenseId,
        String label,
        int quantity,
        Long unitAmount,
        Long lineAmount,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseLineDto of(ExpenseLine expenseLine){
        return new ExpenseLineDto(
                expenseLine.getId(),
                expenseLine.getExpense().getId(),
                expenseLine.getLabel(),
                expenseLine.getQuantity(),
                expenseLine.getUnitAmount(),
                expenseLine.getLineAmount(),
                expenseLine.getCreatedAt(),
                expenseLine.getUpdatedAt()
        );
    }
}
