package com.calendarbox.backend.expense.controller;

import com.calendarbox.backend.expense.dto.request.AddExpenseRequest;
import com.calendarbox.backend.expense.dto.request.EditExpenseRequest;
import com.calendarbox.backend.expense.dto.response.AddExpenseResponse;
import com.calendarbox.backend.expense.dto.response.ExpenseDetailResponse;
import com.calendarbox.backend.expense.dto.response.ExpenseListResponse;
import com.calendarbox.backend.expense.service.ExpenseQueryService;
import com.calendarbox.backend.expense.service.ExpenseService;
import com.calendarbox.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Schedule - Expense", description = "일정 지출")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;
    private final ExpenseQueryService expenseQueryService;

    @Operation(
            summary = "지출 추가",
            description = "해당 스케줄에 지출을 추가합니다.(수기 OR OCR)"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AddExpenseResponse>> addExpense(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @RequestBody @Valid AddExpenseRequest req
            ) {
        var data = expenseService.addExpense(userId,scheduleId,req);
        return ResponseEntity.ok(ApiResponse.ok("지출 등록 성공",data));
    }

    @Operation(
            summary = "지출 목록 조회",
            description = "해당 스케줄의 지출 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<ExpenseListResponse>> getExpenseList(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId
    ){
        var data = expenseQueryService.getExpenses(userId,scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("지출 목록 조회 성공", data));
    }

    @Operation(
            summary = "지출 상세 조회",
            description = "해당 지출의 상세 정보를 조회합니다."
    )
    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDetailResponse>> getExpenseDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long expenseId
    ){
        var data = expenseQueryService.getDetail(userId,scheduleId,expenseId);
        return ResponseEntity.ok(ApiResponse.ok("지출 상세 조회 성공",data));
    }

    @Operation(
            summary = "지출 수정",
            description = "해당 지출을 수정합니다."
    )
    @PatchMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDetailResponse>> editExpense(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long expenseId,
            @RequestBody EditExpenseRequest req
    ){
        var data = expenseService.editExpense(userId,scheduleId,expenseId,req);
        return ResponseEntity.ok(ApiResponse.ok("지출 내용 수정 성공", data));
    }

    @Operation(
            summary = "지출 삭제",
            description = "스케줄에서 해당 지출을 삭제합니다."
    )
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long expenseId
    ){
        expenseService.deleteExpense(userId,scheduleId,expenseId);

        return ResponseEntity.ok(ApiResponse.ok("지출 삭제 성공", null));
    }
}
