package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.CreateScheduleLinkRequest;
import com.calendarbox.backend.schedule.dto.response.ScheduleLinkDto;
import com.calendarbox.backend.schedule.dto.response.ScheduleLinkListResponse;
import com.calendarbox.backend.schedule.service.ScheduleLinkQueryService;
import com.calendarbox.backend.schedule.service.ScheduleLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}/links")
public class ScheduleLinkController {
    private final ScheduleLinkService scheduleLinkService;
    private final ScheduleLinkQueryService scheduleLinkQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleLinkDto>> add(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @RequestBody @Valid CreateScheduleLinkRequest request
    ){
        var data = scheduleLinkService.add(userId,scheduleId,request);
        return ResponseEntity.ok(ApiResponse.ok("스케줄 링크 추가 성공", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ScheduleLinkListResponse>> getList(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {
        var content = scheduleLinkQueryService.getLinks(userId, scheduleId);
        var data = new ScheduleLinkListResponse(content.size(), content);
        return ResponseEntity.ok(ApiResponse.ok("스케줄 링크 목록 조회 성공",data));
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long linkId
    ){
        scheduleLinkService.delete(userId,scheduleId,linkId);

        return ResponseEntity.ok(ApiResponse.ok("스케줄 링크 삭제 성공", null));
    }
}
