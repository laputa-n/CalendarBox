package com.calendarbox.backend.expense.controller;

import com.calendarbox.backend.expense.dto.request.AddExpenseLineRequest;
import com.calendarbox.backend.expense.dto.request.EditExpenseLineRequest;
import com.calendarbox.backend.expense.dto.response.ExpenseLineDto;
import com.calendarbox.backend.expense.dto.response.ExpenseLineListResponse;
import com.calendarbox.backend.expense.service.ExpenseLineQueryService;
import com.calendarbox.backend.expense.service.ExpenseLineService;
import com.calendarbox.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule - Expense", description = "일정 지출")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses/{expenseId}/lines")
public class ExpenseLineController {
    private final ExpenseLineService expenseLineService;
    private final ExpenseLineQueryService expenseLineQueryService;

    @Operation(
            summary = "지출 항목 추가",
            description = "해당 지출에 상세 항목을 추가합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseLineDto>> addExpenseLine(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long expenseId,
            @RequestBody @Valid AddExpenseLineRequest request
            ){
        var data = expenseLineService.addExpenseLine(userId, expenseId, request);
        return ResponseEntity.ok(ApiResponse.ok("지출 항목 추가 성공", data));
    }

    @Operation(
            summary = "지출 항목 수정",
            description = "지출의 해당 항목을 수정합니다."
    )
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

    @Operation(
            summary = "지출 항목 삭제",
            description = "지출에서 해당 항목을 삭제합니다."
    )
    @DeleteMapping("/{expenseLineId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpenseLine(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long expenseId,
            @PathVariable Long expenseLineId
    ) {
        expenseLineService.deleteExpenseLine(userId,expenseId,expenseLineId);

        return ResponseEntity.ok(ApiResponse.ok("지출 항목 삭제 성공",null));
    }

    @Operation(
            summary = "지출 항목 목록 조회",
            description = "해당 지출의 상세 항목 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<ExpenseLineListResponse>> getExpenseLines(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long expenseId
    ){
        var data = expenseLineQueryService.getLines(userId, expenseId);
        return ResponseEntity.ok(ApiResponse.ok("지출 상세 목록 조회 성공", data));
    }
}
