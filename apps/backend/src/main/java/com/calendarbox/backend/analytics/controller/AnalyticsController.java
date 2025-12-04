package com.calendarbox.backend.analytics.controller;

import com.calendarbox.backend.analytics.dto.response.*;
import com.calendarbox.backend.analytics.service.AnalyticsService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

//    @GetMapping("/insight")
//    public ResponseEntity<InsightResponse> getInsight(@RequestParam Long memberId) {
//        InsightResponse response = analyticsService.buildInsight(memberId);
//        return ResponseEntity.ok(response);
//    }

<<<<<<< HEAD
   @GetMapping("/people/summary")
public ResponseEntity<ApiResponse<PeopleStatSummary>> getPeopleSummary(
        @AuthenticationPrincipal(expression = "id") Long userId,
        @RequestParam String yearMonth) {
    
    // 로그 추가 - yearMonth 값 확인
    System.out.println("Received yearMonth: " + yearMonth);  // 로그로 값 확인
    
    YearMonth ym;
    try {
        // YearMonth 파싱 시도
        ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
    } catch (DateTimeParseException e) {
        // 파싱 실패 시 로그 출력
        System.out.println("Error parsing yearMonth: " + yearMonth);
        
        // 상세한 에러 메시지 제공
        throw new BusinessException(ErrorCode.DATETIME_FORMAT_FAIL, "Invalid date format. Expected format: yyyy-MM, but received: " + yearMonth);
=======
    @GetMapping("/people/summary")
    public ResponseEntity<ApiResponse<PeopleStatSummary>> getPeopleSummary(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam String yearMonth
            ){
        log.info("[yearMonth]={}", yearMonth);
        System.out.println("[yearMonth] = " + yearMonth);
        YearMonth ym;
        try{
            ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
            log.info("[parsedYm]={}", String.valueOf(ym));
            System.out.println("[parsedYm] = " + String.valueOf(ym));
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.DATETIME_FORMAT_FAIL);
        }
        var data = analyticsService.getPeopleSummary(userId,ym);
        return ResponseEntity.ok(ApiResponse.ok("사람 통계 요약 성공", data));
>>>>>>> main
    }
    
    // 데이터 처리
    var data = analyticsService.getPeopleSummary(userId, ym);
    return ResponseEntity.ok(ApiResponse.ok("사람 통계 요약 성공", data));
}


    @GetMapping("/people")
    public ResponseEntity<ApiResponse<PageResponse<PeopleStatItem>>> getPeopleStatList(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        var pageResult = analyticsService.getPeopleStatList(userId,yearMonth, page, size);
        var data = PageResponse.of(pageResult);

        return ResponseEntity.ok(ApiResponse.ok("사람 통계 목록 조회 성공", data));
    }

    @GetMapping("/place/summary")
    public ResponseEntity<ApiResponse<PlaceStatSummary>> getPlaceSummary(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ){
        var data = analyticsService.getPlaceSummary(userId,yearMonth);

        return ResponseEntity.ok(ApiResponse.ok("장소 통계 요약 성공", data));
    }

    @GetMapping("/place")
    public ResponseEntity<ApiResponse<PageResponse<PlaceStatItem>>> getPlaceStatList(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        var pageResult = analyticsService.getPlaceStatList(userId,yearMonth,page,size);
        var data = PageResponse.of(pageResult);

        return ResponseEntity.ok(ApiResponse.ok("장소 통계 목록 조회 성공", data));
    }

    @GetMapping("/schedule/day-hour")
    public ResponseEntity<ApiResponse<List<DayHourScheduleDistribution>>> getDayHourScheduleDistribution(
            @AuthenticationPrincipal(expression = "id") Long userId
    ){
        var data = analyticsService.getDayHourScheduleDistribution(userId);

        return ResponseEntity.ok(ApiResponse.ok("요일-시간대 별 스케줄 분포 조회 성공", data));
    }

    @GetMapping("/schedule/trend")
    public ResponseEntity<ApiResponse<List<MonthlyScheduleTrend>>> getMonthlyScheduleTrend(
            @AuthenticationPrincipal(expression = "id") Long userId
    ){
        var data = analyticsService.getMonthlyScheduleTrend(userId);

        return ResponseEntity.ok(ApiResponse.ok("월별 스케줄 추이 조회 성공", data));
    }
}