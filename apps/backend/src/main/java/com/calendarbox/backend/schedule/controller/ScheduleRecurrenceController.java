package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.RecurrenceUpsertRequest;
import com.calendarbox.backend.schedule.dto.response.RecurrenceResponse;
import com.calendarbox.backend.schedule.service.ScheduleRecurrenceQueryService;
import com.calendarbox.backend.schedule.service.ScheduleRecurrenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules/{scheduleId}/recurrences")
@RequiredArgsConstructor
@Validated
public class ScheduleRecurrenceController {

    private final ScheduleRecurrenceQueryService scheduleRecurrenceQueryService;
    private final ScheduleRecurrenceService scheduleRecurrenceService;

    // 목록
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurrenceResponse>>> list(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId
    ) {
        var data = scheduleRecurrenceQueryService.list(userId, scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("반복 목록 조회 성공", data));
    }

    // 생성
    @PostMapping
    public ResponseEntity<ApiResponse<RecurrenceResponse>> create(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody RecurrenceUpsertRequest req
    ) {
        var data = scheduleRecurrenceService.create(userId, scheduleId, req);
        return ResponseEntity.ok(ApiResponse.ok("반복 생성 성공", data));
    }

    // 단건 조회(옵션)
    @GetMapping("/{recurrenceId}")
    public ResponseEntity<ApiResponse<RecurrenceResponse>> get(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId
    ) {
        var data = scheduleRecurrenceQueryService.getOne(userId, scheduleId, recurrenceId);
        return ResponseEntity.ok(ApiResponse.ok("반복 조회 성공", data));
    }

    // 수정
    @PutMapping("/{recurrenceId}")
    public ResponseEntity<ApiResponse<RecurrenceResponse>> update(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId,
            @Valid @RequestBody RecurrenceUpsertRequest req
    ) {
        var data = scheduleRecurrenceService.update(userId, scheduleId, recurrenceId, req);
        return ResponseEntity.ok(ApiResponse.ok("반복 수정 성공", data));
    }

    // 삭제
    @DeleteMapping("/{recurrenceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long recurrenceId
    ) {
        scheduleRecurrenceService.delete(userId, scheduleId, recurrenceId);
        return ResponseEntity.ok(ApiResponse.ok("반복 삭제 성공", null));
    }
}


