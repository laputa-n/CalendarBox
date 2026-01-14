package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.calendar.dto.response.InvitedCalendarMemberItem;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleRequest;
import com.calendarbox.backend.schedule.dto.request.EditScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.*;
import com.calendarbox.backend.schedule.service.ScheduleParticipantQueryService;
import com.calendarbox.backend.schedule.service.ScheduleQueryService;
import com.calendarbox.backend.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Tag(name = "Schedule", description = "스케줄")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScheduleController {
    private final ScheduleQueryService scheduleQueryService;
    private final ScheduleService scheduleService;
    private final ScheduleParticipantQueryService scheduleParticipantQueryService;

    @Operation(
            summary = "스케줄 생성",
            description = "스케줄을 생성합니다."
    )
    @PostMapping("/calendars/{calendarId}/schedules")
    public ResponseEntity<ApiResponse<CreateScheduleResponse>> create(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long calendarId,
            @RequestBody @Valid CreateScheduleRequest request
    ){
        var data = scheduleService.create(userId,calendarId,request);

        return ResponseEntity.ok(ApiResponse.ok("스케줄 생성 성공", data));
    }

    @Operation(
            summary = "스케줄 수정",
            description = "스케줄을 수정합니다."
    )
    @PatchMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDto>> edit(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @RequestBody EditScheduleRequest request
    ){
        var data = scheduleService.edit(userId,scheduleId,request);

        return ResponseEntity.ok(ApiResponse.ok("스케줄 수정 성공",data));
    }

    @Operation(
            summary = "스케줄 삭제",
            description = "스케줄을 삭제합니다."
    )
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ){
        scheduleService.delete(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("스케줄 삭제 성공", null));
    }

    @Operation(
            summary = "스케줄 상세 조회",
            description = "스케줄 상세를 조회합니다."
    )
    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDetailDto>> getDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ){
        var data = scheduleQueryService.getDetail(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("스케줄 상세 조회 성공", data));
    }

    @Operation(
            summary = "스케줄 목록 조회",
            description = "해당 캘린더의 기간 내의 스케줄 목록을 조회합니다."
    )
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
        return ResponseEntity.ok(ApiResponse.ok("스케줄 목록 조회 성공", data));
    }

    @Operation(
            summary = "스케줄 검색",
            description = "스케줄을 검색합니다."
    )
    @GetMapping("/schedules/search")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleListItem>>> search(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @RequestParam(required = false) Long calendarId,
            @RequestParam String query,
            @PageableDefault(size = 30, sort = {"startAt", "id"}) Pageable pageable
    ){
        var pageResult = scheduleQueryService.search(userId,calendarId,query,pageable);
        var data = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.ok("스케줄 검색 성공", data));
    }

    @Operation(
            summary = "받은 스케줄 초대 목록 조회",
            description = "받은 스케줄 초대 목록을 조회합니다."
    )
    @GetMapping("/schedules/invited")
    public ResponseEntity<ApiResponse<PageResponse<InvitedScheduleParticipantItem>>> getInvitedCalendarMemberList(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        size = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invitedAt"));

        var data = scheduleParticipantQueryService.getInvited(userId,pageable);

        return ResponseEntity.ok(ApiResponse.ok("받은 스케줄 초대 목록 조회 성공", data));
    }
}
