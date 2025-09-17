package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.ReminderRequest;
import com.calendarbox.backend.schedule.dto.response.ReminderResponse;
import com.calendarbox.backend.schedule.service.ScheduleReminderQueryService;
import com.calendarbox.backend.schedule.service.ScheduleReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules/{scheduleId}/reminders")
@RequiredArgsConstructor
public class ScheduleReminderController {
    private final ScheduleReminderService scheduleReminderService;
    private final ScheduleReminderQueryService scheduleReminderQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> list(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId
    ){
        var data = scheduleReminderQueryService.list(userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("리마인더 목록 조회 성공", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReminderResponse>> create(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @RequestBody ReminderRequest req
            ){
        var data = scheduleReminderService.create(userId,scheduleId,req);

        return ResponseEntity.ok(ApiResponse.ok("리마인더 생성 성공", data));
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long reminderId
    ){
        scheduleReminderService.delete(userId,scheduleId,reminderId);

        return ResponseEntity.ok(ApiResponse.ok("리마인데 삭제 성공", null));
    }
}
