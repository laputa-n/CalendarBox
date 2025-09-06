package com.calendarbox.backend.calendar.controller;

import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.dto.request.CreateCalendarRequest;
import com.calendarbox.backend.calendar.dto.response.CalendarListItem;
import com.calendarbox.backend.calendar.dto.response.CreateCalendarResponse;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.calendar.service.CalendarQueryService;
import com.calendarbox.backend.calendar.service.CalendarService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendars")
public class CalendarController {
    private final CalendarService calendarService;
    private final CalendarMemberRepository calendarMemberRepository;
    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;
    private final CalendarQueryService calendarQueryService;

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

    @GetMapping("/api/calendars")
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
}
