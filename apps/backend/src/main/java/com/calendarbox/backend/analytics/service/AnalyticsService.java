package com.calendarbox.backend.analytics.service;

import com.calendarbox.backend.analytics.dto.request.MonthlyTrend;
import com.calendarbox.backend.analytics.dto.request.PlaceSummary;
import com.calendarbox.backend.analytics.dto.request.ScheduleSummary;
import com.calendarbox.backend.analytics.dto.response.*;
import com.calendarbox.backend.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AiPredictService aiPredictService;

    @Transactional(readOnly = true)
    public List<MonthlyScheduleTrend> getMonthlyScheduleTrend(Long userId) {
        List<Object[]> monthlyScheduleStats = analyticsRepository.findMonthlyScheduleTrend(userId);

        return monthlyScheduleStats.stream().map(
                row -> new MonthlyScheduleTrend(
                        ((Timestamp)row[0]).toLocalDateTime(),
                        ((Number)row[1]).longValue()
                )
        ).toList();
    }
    @Transactional(readOnly = true)
    public List<DayHourScheduleDistribution> getDayHourScheduleDistribution(Long userId){
        List<Object[]> dayHourScheduleStats = analyticsRepository.findDayHourScheduleDistribution(userId);

        return dayHourScheduleStats.stream().map(
                row -> new DayHourScheduleDistribution(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).longValue()
                )
        ).toList();
    }

    @Transactional(readOnly = true)
    public PlaceStatSummary getPlaceSummary(Long userId, YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> scheduleMonthlySummary = analyticsRepository.findPlaceMonthlyStats(userId, start, end);
        List<Object[]> expenseMonthlySummary = analyticsRepository.findPlaceMonthlyExpenseStats(userId, start, end);

        List<PlaceMonthlyScheduleSummary> scheduleStats = scheduleMonthlySummary.stream()
                .map(row -> new PlaceMonthlyScheduleSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).intValue() : 0,
                        row[4] != null ? ((Number) row[4]).longValue() : 0L
                ))
                .toList();

        List<PlaceMonthlyExpenseSummary> expenseStats = expenseMonthlySummary.stream()
                .map(row -> new PlaceMonthlyExpenseSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).intValue() : 0,
                        row[4] != null ? ((Number) row[4]).longValue() : 0L,
                        row[5] != null ? ((Number) row[5]).longValue() : 0L,
                        row[6] != null ? ((Number) row[6]).doubleValue() : 0.0
                ))
                .toList();

        Map<String, PlaceMonthlyExpenseSummary> expenseMap = expenseStats.stream()
                .collect(Collectors.toMap(
                        e -> (e.placeId() != null ? "ID-" + e.placeId() : "NAME-" + e.placeName()),
                        e -> e,
                        (a,b)->a
                ));

        int totalVisitCount = scheduleStats.stream().mapToInt(PlaceMonthlyScheduleSummary::visitCount).sum();
        long totalStayMin = scheduleStats.stream().mapToLong(PlaceMonthlyScheduleSummary::totalStayTime).sum();

        List<PlaceStatItem> top3 = scheduleStats.stream()
                .sorted(Comparator
                        .comparingInt(PlaceMonthlyScheduleSummary::visitCount).reversed()
                        .thenComparingLong(PlaceMonthlyScheduleSummary::totalStayTime).reversed())
                .limit(3)
                .map(s -> {
                    var key = (s.placeId() != null ? "ID-" + s.placeId() : "NAME-" + s.placeName());
                    var exp = expenseMap.get(key);
                    long totalAmount = exp != null ? exp.totalAmount() : 0L;
                    double avgAmount = exp != null ? exp.avgAmount() : 0.0;
                    double avgStayMin = s.visitCount() > 0 ? Math.round((double) s.totalStayTime() / s.visitCount()) : 0.0;
                    return new PlaceStatItem(
                            s.placeId(),
                            s.placeName(),
                            s.visitCount(),
                            s.totalStayTime(),
                            avgStayMin,
                            totalAmount,
                            avgAmount
                    );
                })
                .toList();

        return new PlaceStatSummary(start, totalVisitCount, totalStayMin, top3);
    }


    @Transactional(readOnly = true)
    public Page<PlaceStatItem> getPlaceStatList(Long userId, YearMonth yearMonth, int page, int size){
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        int offset = page * size;

        List<Object[]> scheduleMonthlySummary = analyticsRepository.findPlaceMonthlyStatsPaged(userId, start, end, size, offset);
        List<Object[]> expenseMonthlySummary = analyticsRepository.findPlaceMonthlyExpenseStatsPaged(userId, start, end, size, offset);

        long total =
                Math.max(
                        analyticsRepository.countPlaceMonthlyStats(userId, start, end),
                        analyticsRepository.countPlaceMonthlyExpenseStats(userId, start, end)
                );

        List<PlaceMonthlyScheduleSummary> scheduleStats = scheduleMonthlySummary.stream()
                .map(row -> new PlaceMonthlyScheduleSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).intValue() : 0,
                        row[4] != null ? ((Number) row[4]).longValue() : 0L
                ))
                .toList();

        List<PlaceMonthlyExpenseSummary> expenseStats = expenseMonthlySummary.stream()
                .map(row -> new PlaceMonthlyExpenseSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).intValue() : 0,
                        row[4] != null ? ((Number) row[4]).longValue() : 0L,
                        row[5] != null ? ((Number) row[5]).longValue() : 0L,
                        row[6] != null ? ((Number) row[6]).doubleValue() : 0.0
                ))
                .toList();

        Map<String, PlaceMonthlyExpenseSummary> expenseMap = expenseStats.stream()
                .collect(Collectors.toMap(
                        e -> (e.placeId() != null ? "ID-" + e.placeId() : "NAME-" + e.placeName()),
                        e -> e,
                        (a,b)->a
                ));

        List<PlaceStatItem> items = scheduleStats.stream()
                .map(s -> {
                    var key = s.placeId() != null ? "ID-" + s.placeId() : "NAME-" + s.placeName();
                    var exp = expenseMap.get(key);
                    return new PlaceStatItem(
                            s.placeId(),
                            s.placeName(),
                            s.visitCount(),
                            s.totalStayTime(),
                            Math.abs((double)s.totalStayTime() / s.visitCount()),
                            exp!=null ? exp.totalAmount() : 0L,
                            (exp!=null && exp.totalAmount() > 0) ? exp.avgAmount() : 0.0
                    );
                }).toList();

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(items, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<PeopleStatItem> getPeopleStatList(Long userId, YearMonth yearMonth, int page, int size) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        int offset = page * size;

        List<Object[]> scheduleSummary =
                analyticsRepository.findPersonMonthlyScheduleStatsPaged(userId, start, end, size, offset);
        List<Object[]> expenseSummary =
                analyticsRepository.findPersonMonthlyExpenseStatsPaged(userId, start, end, size, offset);

        long total =
                Math.max(
                        analyticsRepository.countPersonMonthlyScheduleStats(userId, start, end),
                        analyticsRepository.countPersonMonthlyExpenseStats(userId, start, end)
                );

        List<PersonMonthlyScheduleSummary> scheduleStats = scheduleSummary.stream()
                .map(r -> new PersonMonthlyScheduleSummary(
                        ((Timestamp) r[0]).toLocalDateTime(),
                        r[1] != null ? ((Number) r[1]).longValue() : null,
                        (String) r[2],
                        ((Number) r[3]).intValue(),
                        r[4] != null ? ((Number) r[4]).longValue() : 0L
                ))
                .toList();

        List<PersonMonthlyExpenseSummary> expenseStats = expenseSummary.stream()
                .map(r -> new PersonMonthlyExpenseSummary(
                        ((Timestamp) r[0]).toLocalDateTime(),
                        r[1] != null ? ((Number) r[1]).longValue() : null,
                        (String) r[2],
                        r[3] != null ? ((Number) r[3]).longValue() : 0L,
                        r[4] != null ? ((Number) r[4]).doubleValue() : 0.0,
                        r[5] != null ? ((Number) r[5]).intValue() : 0
                ))
                .toList();

        Map<String,PersonMonthlyExpenseSummary> expenseMap = expenseStats.stream()
                .collect(Collectors.toMap(
                                (e -> e.personId() != null ? "ID-" + e.personId() : "NAME-" + e.personName()),
                        e->e,
                        (a,b)->a
                ));

        List<PeopleStatItem> items = scheduleStats.stream()
                .map(s -> {
                    var key = s.personId() != null ? "ID-" + s.personId() : "NAME-" + s.personName();
                    var exp = expenseMap.get(key);
                    return new PeopleStatItem(
                            s.personId(),
                            s.personName(),
                            s.meetCount(),
                            s.totalDurationMin(),
                            s.meetCount() > 0 ? Math.round((double)s.totalDurationMin() / s.meetCount()) : 0.0,
                            exp != null ? exp.totalAmount() : 0L,
                            (exp != null && exp.sharedScheduleCount() > 0)
                                    ? Math.round((double) exp.totalAmount() / exp.sharedScheduleCount())
                                    : 0.0
                    );
                }).toList();

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(items, pageable, total);
    }

    @Transactional(readOnly = true)
    public PeopleStatSummary getPeopleSummary(Long userId, YearMonth yearMonth){
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> scheduleMonthlySummary = analyticsRepository.findPersonMonthlyScheduleStats(userId, start, end);
        List<Object[]> expenseMonthlySummary = analyticsRepository.findPersonMonthlyExpenseStats(userId, start, end);

        List<PersonMonthlyScheduleSummary> scheduleStats = scheduleMonthlySummary.stream().map(
                row -> new PersonMonthlyScheduleSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        ((Number) row[3]).intValue(),
                        row[4] != null ? ((Number) row[4]).longValue() : 0L
                )).toList();

        List<PersonMonthlyExpenseSummary> expenseStats = expenseMonthlySummary.stream().map(
                row -> new PersonMonthlyExpenseSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).longValue() : 0L,  // totalAmount
                        row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,  // avgAmount
                        row[5] != null ? ((Number) row[5]).intValue() : 0
                )).toList();

        int totalMeetCount = scheduleStats.stream().mapToInt(PersonMonthlyScheduleSummary::meetCount).sum();
        long totalDurationMin = scheduleStats.stream().mapToLong(PersonMonthlyScheduleSummary::totalDurationMin).sum();

        Map<String,PersonMonthlyExpenseSummary> expenseMap = expenseStats.stream().collect(
                Collectors.toMap(
                        e -> e.personId() != null ? "ID-" + e.personId() : "NAME-"+e.personName(),
                        e->e,
                        (a,b)->a
                )
        );
        List<PeopleStatItem> top3 = scheduleStats.stream()
                .sorted(Comparator.comparingLong(PersonMonthlyScheduleSummary::meetCount).reversed())
                .limit(3)
                .map(s ->{
                    var key = s.personId() != null ? "ID-" + s.personId() : "NAME-" + s.personName();
                    var exp = expenseMap.get(key);
                    return new PeopleStatItem(
                            s.personId(),
                            s.personName(),
                            s.meetCount(),
                            s.totalDurationMin(),
                            s.meetCount() > 0 ? Math.round((double) s.totalDurationMin() / s.meetCount()) : 0.0,
                            exp != null ? exp.totalAmount() : 0L,
                            exp != null && exp.sharedScheduleCount() > 0
                                    ? Math.round((double)exp.totalAmount() / exp.sharedScheduleCount())
                                    : 0.0
                    );
                })
                .toList();

        return new PeopleStatSummary(
                start,                   // month
                totalMeetCount,
                totalDurationMin,
                top3
        );
    }

    public InsightResponse buildInsight(Long memberId) {

        // 1️⃣ DB 조회 (nativeQuery 결과는 Object[] 배열)
        List<Object[]> scheduleRows = analyticsRepository.findScheduleSummaries(memberId);
        List<Object[]> placeRows = analyticsRepository.findPlaceStats(memberId);
        List<Object[]> trendRows = analyticsRepository.findMonthlyTrend(memberId);

        // 2️⃣ Object[] → record DTO 변환
        List<ScheduleSummary> schedules = scheduleRows.stream()
                .map(r -> new ScheduleSummary(
                        ((Number) r[0]).longValue(),       // scheduleId
                        (String) r[1],                     // title
                        (String) r[2],                     // placeName
                        ((Number) r[3]).doubleValue(),     // hour
                        ((Number) r[4]).doubleValue(),     // durationMin
                        ((Number) r[5]).doubleValue()      // amount
                ))
                .toList();

        List<PlaceSummary> places = placeRows.stream()
                .map(r -> new PlaceSummary(
                        (String) r[0],
                        ((Number) r[1]).longValue()
                ))
                .toList();

        List<MonthlyTrend> monthlyTrends = trendRows.stream()
                .map(r -> new MonthlyTrend(
                        ((java.sql.Timestamp) r[0]).toLocalDateTime(),
                        ((Number) r[1]).longValue()
                ))
                .toList();

        // 3️⃣ AI 예측
        Map<Long, String> categoryMap = aiPredictService.predictCategories(schedules);

        // 4️⃣ 통합 인사이트 응답 생성
        return InsightResponse.from(schedules, places, monthlyTrends, categoryMap);
    }
}
