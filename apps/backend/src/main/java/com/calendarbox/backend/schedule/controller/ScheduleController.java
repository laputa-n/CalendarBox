package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleRequest;
import com.calendarbox.backend.schedule.dto.request.EditScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.CreateScheduleResponse;
import com.calendarbox.backend.schedule.dto.response.ScheduleDto;
import com.calendarbox.backend.schedule.service.ScheduleQueryService;
import com.calendarbox.backend.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScheduleController {
    private final ScheduleQueryService scheduleQueryService;
    private final ScheduleService scheduleService;

    @PostMapping("/calendars/{calendarId}/schedules")
    public ResponseEntity<ApiResponse<CreateScheduleResponse>> create(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long calendarId,
            @RequestBody @Valid CreateScheduleRequest request
    ){
        var data = scheduleService.create(userId,calendarId,request);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()      // 호출된 URL 기준 (프록시 경로/버전 포함)
                .path("/{id}")
                .buildAndExpand(data.scheduleId())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.ok("일정 생성 성공", data));
    }

    @PatchMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDto>> edit(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @RequestBody EditScheduleRequest request
    ){
        var data = scheduleService.edit(userId,scheduleId,request);

        return ResponseEntity.ok(ApiResponse.ok("일정 수정 성공",data));
    }
}
