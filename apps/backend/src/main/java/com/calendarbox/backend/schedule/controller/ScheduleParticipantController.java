package com.calendarbox.backend.schedule.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.schedule.dto.request.AddParticipantRequest;
import com.calendarbox.backend.schedule.dto.request.ParticipantRespondRequest;
import com.calendarbox.backend.schedule.dto.response.AddParticipantResponse;
import com.calendarbox.backend.schedule.dto.response.ScheduleParticipantResponse;
import com.calendarbox.backend.schedule.enums.AddParticipantMode;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.service.ScheduleParticipantQueryService;
import com.calendarbox.backend.schedule.service.ScheduleParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule - Participant", description = "스케줄 참가자 관리")
@RestController
@RequestMapping("api/schedules/{scheduleId}/participants")
@RequiredArgsConstructor
public class ScheduleParticipantController {
    private final ScheduleParticipantService scheduleParticipantService;
    private final ScheduleParticipantQueryService scheduleParticipantQueryService;

    @Operation(
            summary = "스케줄 참가자 목록 조회",
            description = "스케줄 참가자 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ScheduleParticipantResponse>>> getList(
        @AuthenticationPrincipal(expression="id") Long userId,
        @RequestParam(required = false) ScheduleParticipantStatus status,
        @PathVariable Long scheduleId,
        @PageableDefault(size = 20, sort = {"invitedAt"}) Pageable pageable
    ){
        var page = scheduleParticipantQueryService.list(userId,status,scheduleId, pageable);
        var data = PageResponse.of(page);
        return ResponseEntity.ok(ApiResponse.ok("일정 멤버 목록 조회 성공", data));
    }
    @Operation(
            summary = "스케줄 참가자 추가",
            description = "스케줄 참가자를 초대/추가합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AddParticipantResponse>> addParticipant(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @RequestBody AddParticipantRequest request
    ){
        var data = scheduleParticipantService.add(userId,scheduleId,request);
        String message = "";
        switch(request.mode()){
            case NAME -> message = "일정 멤버 추가 성공";
            case SERVICE_USER -> message = "일정 멤버 초대 성공";
        }
        return ResponseEntity.ok(ApiResponse.ok(message,data));
    }

    @Operation(
            summary = "스케줄 참가자 삭제",
            description = "스케줄 탈퇴/추방합니다."
    )
    @DeleteMapping("/{participantId}")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long participantId
    ){
        scheduleParticipantService.remove(userId,scheduleId,participantId);
        return ResponseEntity.ok(ApiResponse.ok("일정 멤버를 제거했습니다.", null));
    }

    @Operation(
            summary = "스케줄 초대 응답",
            description = "스케줄 초대에 응답합니다."
    )
    @PatchMapping("/{participantId}")
    public ResponseEntity<ApiResponse<ScheduleParticipantResponse>> respond(
            @AuthenticationPrincipal(expression = "id")Long userId,
            @PathVariable Long scheduleId,
            @PathVariable Long participantId,
            @RequestBody ParticipantRespondRequest request
    ){
        var data = scheduleParticipantService.respond(userId,scheduleId,participantId,request);
        String message = "";
        switch(request.action()){
            case ACCEPT -> message = "일정 멤버 초대 수락 성공";
            case REJECT -> message = "일정 멤버 초대 거절 성공";
        }
        return ResponseEntity.ok(ApiResponse.ok(message,data));
    }
}
