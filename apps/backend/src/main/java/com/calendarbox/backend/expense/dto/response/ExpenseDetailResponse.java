package com.calendarbox.backend.expense.dto.response;

import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.enums.ExpenseSource;
import com.calendarbox.backend.expense.enums.ReceiptParseStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record ExpenseDetailResponse(
        Long expenseId,
        Long scheduleId,
        String name,
        Long amount,
        Instant paidAt,
        LocalDate occurrenceDate,
        Instant createdAt,
        Instant updatedAt,
        ExpenseSource source,
        ReceiptParseStatus receiptParseStatus,
        Map<String,Object> parsedPayload
) {
    public static ExpenseDetailResponse from(Expense expense) {
        return new ExpenseDetailResponse(
                expense.getId(),
                expense.getSchedule().getId(),
                expense.getName(),
                expense.getAmount(),
                expense.getPaidAt(),
                expense.getOccurrenceDate(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.getSource(),
                expense.getReceiptParseStatus(),
                expense.getParsedPayload()
        );
    }
}
