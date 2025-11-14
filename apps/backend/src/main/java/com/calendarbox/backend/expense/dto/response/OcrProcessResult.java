package com.calendarbox.backend.expense.dto.response;

import com.calendarbox.backend.expense.domain.Expense;

import java.util.Map;

public record OcrProcessResult(
        Map<String, Object> raw,
        Map<String, Object> normalized,
        Expense expense
) {}
