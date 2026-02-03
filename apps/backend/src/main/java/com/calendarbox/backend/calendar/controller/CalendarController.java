package com.calendarbox.backend.calendar.controller;

import com.calendarbox.backend.calendar.dto.request.*;
import com.calendarbox.backend.calendar.dto.response.*;
import com.calendarbox.backend.calendar.enums.CalendarMemberSort;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.service.*;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.schedule.dto.request.CloneScheduleRequest;
import com.calendarbox.backend.schedule.dto.response.CloneScheduleResponse;
import com.calendarbox.backend.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Calendar", description = "캘린더")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CalendarController {
    private final CalendarService calendarService;
    private final CalendarQueryService calendarQueryService;
    private final CalendarMemberService calendarMemberService;
    private final CalendarMemberQueryService calendarMemberQueryService;
    private final ScheduleService scheduleService;
    private final CalendarHistoryQueryService calendarHistoryQueryService;

    @Operation(summary = "캘린더 생성", description = "새 캘린더를 생성합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "생성 성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "success",
                            value = """
                        {
                          "code": 200,
                          "status": "OK",
                          "message": "캘린더가 생성되었습니다.",
                          "data": {
                            "calendarId": 1,
                            "name": "개인",
                            "type": "PERSONAL",
                            "visibility": "PRIVATE",
                            "isDefault": true
                          }
                        }
                        """
                    )
            )
    )
    @PostMapping("/calendars")
    public ResponseEntity<ApiResponse<CreateCalendarResponse>> createCalendar(
            @AuthenticationPrincipal (expression = "id") Long userId,
            @Valid @RequestBody CreateCalendarRequest request
    ){
        var data = calendarService.create(userId, request.name(), request.type(), request.visibility(), request.isDefault());

        return ResponseEntity.ok(ApiResponse.ok("캘린더가 생성되었습니다.", data));
    }

    @Operation(
            summary = "캘린더 목록 조회",
            description = "내 캘린더 목록을 조회합니다."
    )
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

    @Operation(
            summary = "캘린더 상세 조회",
            description = "해당 캘린더 상세 정보를 조회합니다."
    )
    @GetMapping("/calendars/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarDetail>> getDetail(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        var data = calendarQueryService.getCalendarDetail(userId, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 상세 조회 성공", data));
    }

    @Operation(
            summary = "캘린더 수정",
            description = "해당 캘린더를 수정합니다."
    )
    @PatchMapping("/calendars/{calendarId}")
    public ResponseEntity<ApiResponse<CalendarEditResponse>> editCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @Valid @RequestBody CalendarEditRequest request
    ){
        var data = calendarService.editCalendar(userId, calendarId, request);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 수정 성공", data));
    }

    @Operation(
            summary = "캘린더 삭제",
            description = "해당 캘린더를 삭제합니다."
    )
    @DeleteMapping("/calendars/{calendarId}")
    public ResponseEntity<Void> deleteCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        calendarService.deleteCalendar(userId,calendarId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "캘린더 멤버 초대",
            description = "캘린더 멤버를 초대합니다."
    )
    @PostMapping("/calendars/{calendarId}/members")
    public ResponseEntity<ApiResponse<InviteMembersResponse>> inviteMember(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @Valid @RequestBody InviteMembersRequest request
            ){
        var data = calendarMemberService.inviteMembers(userId, calendarId, request);

        return ResponseEntity.ok(ApiResponse.ok("캘린더 초대 성공",data));
    }

    @Operation(
            summary = "캘린더 멤버 목록 조회",
            description = "해당 캘린더의 멤버 목록윽 조회합니다."
    )
    @GetMapping("/calendars/{calendarId}/members")
    public ResponseEntity<ApiResponse<PageResponse<CalendarMemberItem>>> getCalendarMemberList(
            @AuthenticationPrincipal(expression = "id")Long viewerId,
            @PathVariable Long calendarId,
            @RequestParam(required = false) CalendarMemberStatus status,
            @RequestParam(required = false, defaultValue = "NAME_ASC")CalendarMemberSort sort,
            Pageable pageable
            ){
        var pageResult = calendarMemberQueryService.listMembers(viewerId, calendarId, status, sort, pageable);
        var data = PageResponse.of(pageResult);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 멤버 목록 조회 성공", data));
    }

    @Operation(
            summary = "기본 캘린더 설정",
            description = "해당 캘린더를 기본 캘린더로 설정합니다."
    )
    @PatchMapping("/calendars/{calendarId}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId
    ){
        calendarService.setDefault(userId, calendarId);
        return ResponseEntity.ok(ApiResponse.ok("기본 캘린더 설정 완료",null));
    }

    @Operation(
            summary = "캘린더 맴버 초대 응답",
            description = "캘린더 멤버 초대에 응답합니다."
    )
    @PatchMapping("/calendar-members/{calendarMemberId}")
    public ResponseEntity<ApiResponse<CalendarInviteRespondResponse>> respondToCalendarInvite(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarMemberId,
            @RequestBody @Valid CalendarInvitedRespondRequest request
            ){
        var data = calendarMemberService.respond(userId,calendarMemberId,request);

        return ResponseEntity.ok(ApiResponse.ok("캘린더 초대 응답 성공", data));
    }

    @Operation(
            summary = "캘린더 맴버 강퇴/탈퇴",
            description = "해당 캘린더에서 강퇴/탈퇴합니다."
    )
    @DeleteMapping("/calendar-members/{calendarMemberId}")
    public ResponseEntity<ApiResponse<Void>> deleteCalendarMember(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarMemberId
    ) {
        String msg = calendarMemberService.deleteCalendarMember(userId, calendarMemberId);
        return ResponseEntity.ok(ApiResponse.ok(msg,null));
    }

    @Operation(
            summary = "스케줄 복제",
            description = "해당 캘린더의 스케줄을 복제합니다."
    )
    @PostMapping("/calendars/{calendarId}/schedules/clone")
    public ResponseEntity<ApiResponse<CloneScheduleResponse>> cloneToCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @RequestBody CloneScheduleRequest request
    ){
        var data = scheduleService.clone(userId, calendarId, request);

        return ResponseEntity.ok(ApiResponse.ok("일정 복제 성공", data));
    }

    @Operation(
            summary = "캘린더 히스토리 조회",
            description = "캘린더 히스토리 목록을 조회합니다."
    )
    @GetMapping("/calendars/{calendarId}/histories")
    public ResponseEntity<ApiResponse<PageResponse<CalendarHistoryDto>>> getHistories(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long calendarId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ){
        var pageResult = calendarHistoryQueryService.getHistories(userId,calendarId,from,to, pageable);
        var data = PageResponse.of(pageResult);

        return ResponseEntity.ok(ApiResponse.ok("캘린더 히스토리 조회 성공",data));
    }

    @Operation(
            summary = "받은 캘린더 초대 목록 조회",
            description = "받은 캘린더 초대 목록을 조회합니다."
    )
    @GetMapping("/calendars/invited")
    public ResponseEntity<ApiResponse<PageResponse<InvitedCalendarMemberItem>>> getInvitedCalendarMemberList(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        size = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        var data = calendarMemberQueryService.getInvited(userId,pageable);

        return ResponseEntity.ok(ApiResponse.ok("받은 캘린더 초대 목록 조회 성공", data));
    }
}
