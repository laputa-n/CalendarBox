package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleRequest;
import com.calendarbox.backend.schedule.dto.request.EditScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.*;
import com.calendarbox.backend.schedule.service.ScheduleQueryService;
import com.calendarbox.backend.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

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

        return ResponseEntity.ok(ApiResponse.ok("일정 생성 성공", data));
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

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ){
        scheduleService.delete(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("일정 삭제 성공", null));
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDetailDto>> getDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ){
        var data = scheduleQueryService.getDetail(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("일정 상세 조회 성공", data));
    }

    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleListItem>>> getList(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(required = false) Long calendarId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 30, sort = {"startAt","id"}) Pageable pageable
    ){
        var pageResult = scheduleQueryService.getList(userId,calendarId,from,to,pageable);
        var data = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.ok("일정 목록 조회 성공", data));
    }

    @GetMapping("/schedules/search")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleListItem>>> search(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @RequestParam(required = false) Long calendarId,
            @RequestParam String query,
            @PageableDefault(size = 30, sort = {"startAt", "id"}) Pageable pageable
    ){
        var pageResult = scheduleQueryService.search(userId,calendarId,query,pageable);
        var data = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.ok("일정 검색 성공", data));
    }
}
