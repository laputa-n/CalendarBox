package com.calendarbox.backend.expense.controller;

import com.calendarbox.backend.expense.dto.request.AddExpenseLineRequest;
import com.calendarbox.backend.expense.dto.request.EditExpenseLineRequest;
import com.calendarbox.backend.expense.dto.response.ExpenseLineDto;
import com.calendarbox.backend.expense.dto.response.ExpenseLineListResponse;
import com.calendarbox.backend.expense.service.ExpenseLineQueryService;
import com.calendarbox.backend.expense.service.ExpenseLineService;
import com.calendarbox.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses/{expenseId}/lines")
public class ExpenseLineController {
    private final ExpenseLineService expenseLineService;
    private final ExpenseLineQueryService expenseLineQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseLineDto>> addExpenseLine(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long expenseId,
            @RequestBody @Valid AddExpenseLineRequest request
            ){
        var data = expenseLineService.addExpenseLine(userId, expenseId, request);
        return ResponseEntity.ok(ApiResponse.ok("지출 항목 추가 성공", data));
    }

    @PatchMapping("/{expenseLineId}")
    public ResponseEntity<ApiResponse<ExpenseLineDto>> editExpenseLine(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long expenseId,
            @PathVariable Long expenseLineId,
            @RequestBody @Valid EditExpenseLineRequest request
    ) {
        var data = expenseLineService.editExpenseLine(userId, expenseId, expenseLineId, request);
        return ResponseEntity.ok(ApiResponse.ok("지출 항목 수정 성공", data));
    }

    @DeleteMapping("/{expenseLineId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpenseLine(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long expenseId,
            @PathVariable Long expenseLineId
    ) {
        expenseLineService.deleteExpenseLine(userId,expenseId,expenseLineId);

        return ResponseEntity.ok(ApiResponse.ok("지출 상세 항목 삭제 성공",null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ExpenseLineListResponse>> getExpenseLines(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long expenseId
    ){
        var data = expenseLineQueryService.getLines(userId, expenseId);
        return ResponseEntity.ok(ApiResponse.ok("지출 상세 목록 조회 성공", data));
    }
}
