package com.calendarbox.backend.calendar.controller;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.dto.request.CalendarEditRequest;
import com.calendarbox.backend.calendar.dto.request.CreateCalendarRequest;
import com.calendarbox.backend.calendar.dto.request.InviteMembersRequest;
import com.calendarbox.backend.calendar.dto.response.*;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.service.CalendarMemberService;
import com.calendarbox.backend.calendar.service.CalendarQueryService;
import com.calendarbox.backend.calendar.service.CalendarService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendars")
public class CalendarController {
    private final CalendarService calendarService;
    private final CalendarQueryService calendarQueryService;
    private final CalendarMemberService calendarMemberService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateCalendarResponse>> createCalendar(
            @AuthenticationPrincipal (expression = "id") Long userId,
            @Valid @RequestBody CreateCalendarRequest request
    ){
        Calendar c = calendarService.create(userId, request.name(), request.type(), request.visibility(), request.isDefault());
        var data = new CreateCalendarResponse(
                        c.getId(),
                        c.getOwner().getId(),
                        c.getName(),
                        c.getType(),
                        c.getVisibility(),
                        c.getCreatedAt()
                );

        URI location = URI.create("/api/calendars/" + c.getId());
        return ResponseEntity.created(location).body(ApiResponse.ok("캘린더가 생성되었습니다.", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CalendarListItem>>> list(
            @AuthenticationPrincipal(expression = "id") Long viewerId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) CalendarType type,
            @RequestParam(required = false) CalendarMemberStatus status,
            @RequestParam(required = false) Visibility visibility,
            Pageable pageable
    ) {
        var page = calendarQueryService.listCalendars(viewerId, memberId, type, status, visibility, pageable);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 목록 조회 성공", page));
    }

    @GetMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarDetail>> getDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        var data = calendarQueryService.getCalendarDetail(userId, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 상세 조회 성공", data));
    }

    @PatchMapping("/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarEditResponse>> editCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @Valid @RequestBody CalendarEditRequest request
    ){
        var data = calendarService.editCalendar(userId, calendarId, request);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 수정 성공", data));
    }

    @DeleteMapping("/{calendarId}")
    public ResponseEntity<Void> deleteCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        calendarService.deleteCalendar(userId,calendarId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{calendarId}/members")
    public ResponseEntity<ApiResponse<InviteMembersResponse>> inviteMember(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @Valid @RequestBody InviteMembersRequest request
            ){
        InviteMembersResponse response = calendarMemberService.inviteMembers(userId, calendarId, request);

        return ResponseEntity.ok(ApiResponse.ok("캘린더 초대 성공",response));
    }
}
