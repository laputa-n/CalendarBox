package com.calendarbox.backend.expense.dto.response;

import java.util.List;

public record ExpenseListResponse(
        int count,
        List<ExpenseListItem> content
) {
    public static ExpenseListResponse of(List<ExpenseListItem> items) {
        return new ExpenseListResponse(items.size(), items);
    }
}
