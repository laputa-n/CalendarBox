package com.calendarbox.backend.calendar.controller;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.dto.request.*;
import com.calendarbox.backend.calendar.dto.response.*;
import com.calendarbox.backend.calendar.enums.CalendarMemberSort;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.service.CalendarMemberQueryService;
import com.calendarbox.backend.calendar.service.CalendarMemberService;
import com.calendarbox.backend.calendar.service.CalendarQueryService;
import com.calendarbox.backend.calendar.service.CalendarService;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.CloneScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.CloneScheduleResponse;
import com.calendarbox.backend.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CalendarController {
    private final CalendarService calendarService;
    private final CalendarQueryService calendarQueryService;
    private final CalendarMemberService calendarMemberService;
    private final CalendarMemberQueryService calendarMemberQueryService;
    private final ScheduleService scheduleService;

    @PostMapping("/calendars")
    public ResponseEntity<ApiResponse<CreateCalendarResponse>> createCalendar(
            @AuthenticationPrincipal (expression = "id") Long userId,
            @Valid @RequestBody CreateCalendarRequest request
    ){
        var data = calendarService.create(userId, request.name(), request.type(), request.visibility(), request.isDefault());

        return ResponseEntity.ok(ApiResponse.ok("캘린더가 생성되었습니다.", data));
    }

    @GetMapping("/calendars")
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

    @GetMapping("/calendars/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarDetail>> getDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        var data = calendarQueryService.getCalendarDetail(userId, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 상세 조회 성공", data));
    }

    @PatchMapping("/calendars/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarEditResponse>> editCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @Valid @RequestBody CalendarEditRequest request
    ){
        var data = calendarService.editCalendar(userId, calendarId, request);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 수정 성공", data));
    }

    @DeleteMapping("/calendars/{calendarId}")
    public ResponseEntity<Void> deleteCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        calendarService.deleteCalendar(userId,calendarId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calendars/{calendarId}/members")
    public ResponseEntity<ApiResponse<InviteMembersResponse>> inviteMember(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @Valid @RequestBody InviteMembersRequest request
            ){
        var data = calendarMemberService.inviteMembers(userId, calendarId, request);

        return ResponseEntity.ok(ApiResponse.ok("캘린더 초대 성공",data));
    }

    @GetMapping("/calendars/{calendarId}/members")
    public ResponseEntity<ApiResponse<PageResponse<CalendarMemberItem>>> getCalendarMemberList(
            @AuthenticationPrincipal(expression = "id")Long viewerId,
            @PathVariable Long calendarId,
            @RequestParam(required = false) CalendarMemberStatus status,
            @RequestParam(required = false, defaultValue = "NAME_ASC")CalendarMemberSort sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
            ){
        var pageResult = calendarMemberQueryService.listMembers(viewerId, calendarId, status, sort, PageRequest.of(page,size));
        var data = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 멤버 목록 조회 성공", data));
    }

    @PutMapping("/calendars/{calendarId}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        calendarService.setDefault(userId, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("기본 캘린더 설정 완료",null));
    }

    @PatchMapping("/calendar-members/{calendarMemberId}")
    public ResponseEntity<ApiResponse<CalendarInviteRespondResponse>> respondToCalendarInvite(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarMemberId,
            @RequestBody @Valid CalendarInvitedRespondRequest request
            ){
        var data = calendarMemberService.respond(userId,calendarMemberId,request);

        return ResponseEntity.ok(ApiResponse.ok("캘린더 초대 응답 성공", data));
    }

    @DeleteMapping("/calendar-members/{calendarMemberId}")
    public ResponseEntity<ApiResponse<DeleteCalendarMemberResponse>> deleteCalendarMember(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarMemberId
    ) {
        boolean isWithdraw = calendarMemberService.deleteCalendarMember(userId, calendarMemberId);
        String msg = isWithdraw? "캘린더에서 탈퇴했습니다." : "멤버를 캘린더에서 추방시켰습니다.";

        var data = new DeleteCalendarMemberResponse(isWithdraw);
        return ResponseEntity.ok(ApiResponse.ok(msg,data));
    }

    @PostMapping("/calendars/{calendarId}/schedules/clone")
    public ResponseEntity<ApiResponse<CloneScheduleResponse>> cloneToCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @RequestBody CloneScheduleRequest request
    ){
        var data = scheduleService.clone(userId, calendarId, request);

        return ResponseEntity.ok(ApiResponse.ok("일정 복제 성공", data));
    }
}
