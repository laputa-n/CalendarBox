package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.RecurrenceExceptionRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceExceptionResponse;
import com.calendarbox.backend.schedule.service.RecurrenceExceptionQueryService;
import com.calendarbox.backend.schedule.service.RecurrenceExceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules/{scheduleId}/recurrences/{recurrenceId}/exceptions")
@RequiredArgsConstructor
public class RecurrenceExceptionController {
    private final RecurrenceExceptionQueryService recurrenceExceptionQueryService;
    private final RecurrenceExceptionService recurrenceExceptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurrenceExceptionResponse>>> list(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId
    ) {
        var data = recurrenceExceptionQueryService.list(userId, recurrenceId);
        return ResponseEntity.ok(ApiResponse.ok("예외 목록 조회 성공", data));
    }

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

