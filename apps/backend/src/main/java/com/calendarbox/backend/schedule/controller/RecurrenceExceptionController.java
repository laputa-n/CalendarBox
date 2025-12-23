package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.RecurrenceExceptionRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceExceptionResponse;
import com.calendarbox.backend.schedule.service.RecurrenceExceptionQueryService;
import com.calendarbox.backend.schedule.service.RecurrenceExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule - Recurrence", description = "스케줄 반복 규칙 및 예외(EXDATE)")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/recurrences/{recurrenceId}/exceptions")
@RequiredArgsConstructor
public class RecurrenceExceptionController {
    private final RecurrenceExceptionQueryService recurrenceExceptionQueryService;
    private final RecurrenceExceptionService recurrenceExceptionService;

    @Operation(
            summary = "반복 예외 목록 조회",
            description = "해당 반복의 예외 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurrenceExceptionResponse>>> list(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId
    ) {
        var data = recurrenceExceptionQueryService.list(userId, recurrenceId);
        return ResponseEntity.ok(ApiResponse.ok("예외 목록 조회 성공", data));
    }

    @Operation(
            summary = "반복 예외 추가",
            description = "해당 반복에 예외를 추가합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<RecurrenceExceptionResponse>> add(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId,
            @Valid @RequestBody RecurrenceExceptionRequest req
    ) {
        var data = recurrenceExceptionService.add(userId, recurrenceId, req);
        return ResponseEntity.ok(ApiResponse.ok("예외 추가 성공", data));
    }

    @Operation(
            summary = "반복 예외 삭제",
            description = "해당 반복의 예외를 삭제합니다."
    )
    @DeleteMapping("/{exceptionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId,
            @PathVariable Long exceptionId
    ) {
        recurrenceExceptionService.delete(userId, exceptionId);
        return ResponseEntity.ok(ApiResponse.ok("예외 삭제 성공",null));
    }
}

