package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.AddSchedulePlaceRequest;
import com.calendarbox.backend.schedule.dto.request.PlaceReorderRequest;
import com.calendarbox.backend.schedule.dto.request.SchedulePlaceEditRequest;
import com.calendarbox.backend.schedule.dto.response.SchedulePlaceDto;
import com.calendarbox.backend.schedule.service.SchedulePlaceQueryService;
import com.calendarbox.backend.schedule.service.SchedulePlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule - Place", description = "스케줄 장소")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/places")
@RequiredArgsConstructor
public class SchedulePlaceController {
    private final SchedulePlaceService schedulePlaceService;
    private final SchedulePlaceQueryService schedulePlaceQueryService;

    @Operation(
            summary = "스케줄 장소 목록 조회",
            description = "스케줄 장소 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<SchedulePlaceDto>>> getSchedulePlaces(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ){
        var data = schedulePlaceQueryService.getSchedulePlaces(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("스케줄 장소 목록 조회 성공", data));
    }

    @Operation(
            summary = "스케줄 장소 추가",
            description = "스케줄 장소를 추가합니다.(검색 OR 수기)"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SchedulePlaceDto>> create(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody AddSchedulePlaceRequest req
            ){
        var data = schedulePlaceService.addPlace(userId,scheduleId,req);

        return ResponseEntity.ok(ApiResponse.ok("일정 장소 추가 성공",data));
    }

    @Operation(
            summary = "스케줄 장소 삭제",
            description = "스케줄 장소를 삭제합니다."
    )
    @DeleteMapping("/{schedulePlaceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long schedulePlaceId
    ){
        schedulePlaceService.delete(userId,scheduleId,schedulePlaceId);

        return ResponseEntity.ok(ApiResponse.ok("일정 장소 삭제 성공",null));
    }

    @Operation(
            summary = "스케줄 장소 상세 조회",
            description = "스케줄 장소 상세를 조회합니다."
    )
    @GetMapping("/{schedulePlaceId}")
    public ResponseEntity<ApiResponse<SchedulePlaceDto>> getDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long schedulePlaceId
    ){
        var data = schedulePlaceQueryService.getDetail(userId,scheduleId,schedulePlaceId);

        return ResponseEntity.ok(ApiResponse.ok("일정 장소 상세 조회 성공",data));
    }

    @Operation(
            summary = "스케줄 장소 수정",
            description = "스케줄 장소 정보를 수정합니다."
    )
    @PatchMapping("/{schedulePlaceId}")
    public ResponseEntity<ApiResponse<SchedulePlaceDto>> edit(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long schedulePlaceId,
            @RequestBody @Valid SchedulePlaceEditRequest req
    ){
        var data = schedulePlaceService.edit(userId,scheduleId,schedulePlaceId,req);

        return ResponseEntity.ok(ApiResponse.ok("일정 장소 이름 수정 성공",data));
    }

    @Operation(
            summary = "스케줄 장소 순서 재정렬",
            description = "스케줄 장소 순서를 재정렬합니다."
    )
    @PatchMapping
    public ResponseEntity<ApiResponse<List<SchedulePlaceDto>>> reorder(
        @AuthenticationPrincipal(expression = "id") Long userId,
        @PathVariable Long scheduleId,
        @RequestBody PlaceReorderRequest req
    ){
        var data = schedulePlaceService.reorder(userId,scheduleId,req);
        return ResponseEntity.ok(ApiResponse.ok("일정 장소 순서 재정렬 성공",data));
    }
    // 순서재설정
}
