package com.calendarbox.backend.analytics.dto.response;

import com.calendarbox.backend.analytics.dto.request.MonthlyTrend;
import com.calendarbox.backend.analytics.dto.request.PlaceSummary;
import com.calendarbox.backend.analytics.dto.request.ScheduleSummary;

import java.util.*;
import java.util.stream.Collectors;

public record InsightResponse(
        List<Map<String, Object>> monthlyTrend,
        List<Map<String, Object>> placeStats,
        List<Map<String, Object>> expenseByCategory
) {

    public static InsightResponse from(
            List<ScheduleSummary> schedules,
            List<PlaceSummary> places,
            List<MonthlyTrend> trends,
            Map<Long, String> categoryMap
    ) {
        // 1️⃣ 월별 카테고리별 일정 수
        Map<String, Map<String, Long>> monthlyCategoryCount = schedules.stream()
                .collect(Collectors.groupingBy(
                        s -> s.scheduleId().toString().substring(0, 7), // 임시 월 추출
                        Collectors.groupingBy(
                                s -> categoryMap.getOrDefault(s.scheduleId(), "기타"),
                                Collectors.counting()
                        )
                ));

        List<Map<String, Object>> monthlyTrendList = monthlyCategoryCount.entrySet().stream()
                .map(e -> Map.of(
                        "month", e.getKey(),
                        "category_breakdown", e.getValue()
                ))
                .toList();

        // 2️⃣ 장소별 카테고리 비율
        Map<String, Map<String, Long>> placeCategoryCount = schedules.stream()
                .filter(s -> s.placeName() != null)
                .collect(Collectors.groupingBy(
                        ScheduleSummary::placeName,
                        Collectors.groupingBy(
                                s -> categoryMap.getOrDefault(s.scheduleId(), "기타"),
                                Collectors.counting()
                        )
                ));

        List<Map<String, Object>> placeStatsList = placeCategoryCount.entrySet().stream()
                .map(e -> Map.of(
                        "place_name", e.getKey(),
                        "category_ratio", e.getValue()
                ))
                .toList();

        // 3️⃣ 카테고리별 지출 합계 및 평균
        Map<String, DoubleSummaryStatistics> expenseStats = schedules.stream()
                .collect(Collectors.groupingBy(
                        s -> categoryMap.getOrDefault(s.scheduleId(), "기타"),
                        Collectors.summarizingDouble(ScheduleSummary::amount)
                ));

        List<Map<String, Object>> expenseByCategoryList = expenseStats.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", e.getKey());
                    map.put("total_amount", e.getValue().getSum());
                    map.put("avg_amount", e.getValue().getAverage());
                    return map;
                })
                .toList();



        return new InsightResponse(monthlyTrendList, placeStatsList, expenseByCategoryList);
    }
}
