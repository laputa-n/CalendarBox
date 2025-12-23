package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.TodoCreateRequest;
import com.calendarbox.backend.schedule.dto.request.TodoReorderRequest;
import com.calendarbox.backend.schedule.dto.request.TodoUpdateRequest;
import com.calendarbox.backend.schedule.dto.response.TodoResponse;
import com.calendarbox.backend.schedule.service.ScheduleTodoQueryService;
import com.calendarbox.backend.schedule.service.ScheduleTodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule - Todo", description = "스케줄 투두")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/todos")
@RequiredArgsConstructor
public class ScheduleTodoController {
    private final ScheduleTodoService scheduleTodoService;
    private final ScheduleTodoQueryService scheduleTodoQueryService;

    @Operation(
            summary = "스케줄 투두 목록 조회",
            description = "스케줄 투두 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoResponse>>> list(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long scheduleId
    ){
        var data = scheduleTodoQueryService.list(userId,scheduleId);
        return ResponseEntity.ok(ApiResponse.ok("투두 목록 조회 성공",data));
    }

    @Operation(
            summary = "스케줄 투두 추가",
            description = "스케줄 투두를 추가합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> add(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId,
            @RequestBody TodoCreateRequest req
    ){
        var data = scheduleTodoService.addAtBottom(userId,scheduleId,req);
        return ResponseEntity.ok(ApiResponse.ok("투두 추가 성공", data));
    }

    @Operation(
            summary = "스케줄 투두 수정",
            description = "스케줄 투두를 수정합니다."
    )
    @PatchMapping("/{scheduleTodoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> edit(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long scheduleTodoId,
            @RequestBody TodoUpdateRequest request
            ){
        var data = scheduleTodoService.updateContent(userId,scheduleId,scheduleTodoId,request);
        return ResponseEntity.ok(ApiResponse.ok("투두 내용 수정 성공",data));
    }

    @Operation(
            summary = "스케줄 투두 토글",
            description = "스케줄 투두를 완료/미완료 처리합니다."
    )
    @PatchMapping("/{scheduleTodoId}/toggle")
    public ResponseEntity<ApiResponse<TodoResponse>> toggle(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long scheduleTodoId
    ){
        var data = scheduleTodoService.toggle(userId,scheduleId,scheduleTodoId);

        return ResponseEntity.ok(ApiResponse.ok("투두 토글 성공",data));
    }

    @Operation(
            summary = "스케줄 투두 순서 재정렬",
            description = "스케줄 투두 순서를 재정렬합니다."
    )
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> reorder(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @RequestBody TodoReorderRequest request
            ){
        scheduleTodoService.reorder(userId,scheduleId,request);
        var data =scheduleTodoQueryService.list(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("투두 재정렬 성공", data));
    }

    @Operation(
            summary = "스케줄 투두 삭제",
            description = "스케줄 투두를 삭제합니다."
    )
    @DeleteMapping("/{scheduleTodoId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long scheduleTodoId
    ){
        scheduleTodoService.delete(userId, scheduleId, scheduleTodoId);
        return ResponseEntity.ok(ApiResponse.ok("투두 삭제 성공",null));
    }
}
