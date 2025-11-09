package com.calendarbox.backend.expense.dto.response;

import java.util.List;

public record ExpenseListResponse(
        int count,
        long totalAmount,
        List<ExpenseListItem> expenses
) { }
