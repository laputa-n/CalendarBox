package com.calendarbox.backend.occurrence.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.occurrence.dto.response.OccurrenceBucketResponse;
import com.calendarbox.backend.occurrence.service.OccurrenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OccurrenceController {

    private final OccurrenceQueryService occurrenceQueryService;

    // 특정 캘린더
    @GetMapping("/calendars/{calendarId}/occurrences")
    public ResponseEntity<ApiResponse<OccurrenceBucketResponse>> getOccurrencesByCalendar(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long calendarId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to
    ) {
        if (!from.isBefore(to)) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
        ZoneId zone = ZoneId.of("Asia/Seoul");
        var resp = occurrenceQueryService.getOccurrences(userId, /*nullable*/ calendarId, from, to, zone);
        return ResponseEntity.ok(ApiResponse.ok("캘린더 occurrence 조회 성공", resp));
    }

    // 모든 캘린더 - 메인 대시보드
    @GetMapping("/occurrences")
    public ResponseEntity<ApiResponse<OccurrenceBucketResponse>> getOccurrencesAll(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to
    ) {
        if (!from.isBefore(to)) throw new BusinessException(ErrorCode.START_AFTER_BEFORE);
        ZoneId zone = ZoneId.of("Asia/Seoul");
        var resp = occurrenceQueryService.getOccurrences(userId, /*calendarId=*/ null, from, to, zone);
        return ResponseEntity.ok(ApiResponse.ok("전체 occurrence 조회 성공", resp));
    }
}
