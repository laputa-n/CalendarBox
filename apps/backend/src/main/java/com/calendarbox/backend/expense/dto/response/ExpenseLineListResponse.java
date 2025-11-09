package com.calendarbox.backend.expense.dto.response;

import com.calendarbox.backend.expense.domain.Expense;

import java.util.List;

public record ExpenseLineListResponse(
        int count,
        List<ExpenseLineDto> lines
) {
    public static ExpenseLineListResponse from(Expense expense) {
        return new ExpenseLineListResponse(
                expense.getLines().size(),
                expense.getLines().stream().map(
                        ExpenseLineDto::of
                ).toList()
        );
    }
}
