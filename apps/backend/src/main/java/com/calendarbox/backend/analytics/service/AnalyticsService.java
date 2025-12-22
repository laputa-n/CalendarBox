package com.calendarbox.backend.analytics.service;

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

    private record PlaceExpenseAgg(long totalAmount, double avgAmount) {}
    private record ExpenseAgg(long totalAmount, double avgAmount) {} // (people에도 재사용해도 됨)

    @Transactional(readOnly = true)
    public PlaceStatSummary getPlaceSummary(Long userId, YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> scheduleMonthlySummary = analyticsRepository.findPlaceMonthlyStats(userId, start, end);

        List<PlaceMonthlyScheduleSummary> scheduleStats = scheduleMonthlySummary.stream()
                .map(row -> new PlaceMonthlyScheduleSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        row[1] != null ? ((Number) row[1]).longValue() : null,
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).intValue() : 0,
                        row[4] != null ? ((Number) row[4]).longValue() : 0L
                ))
                .toList();

        List<PlaceMonthlyScheduleSummary> top3Base = scheduleStats.stream()
                .sorted(Comparator
                        .comparingInt(PlaceMonthlyScheduleSummary::visitCount).reversed()
                        .thenComparingLong(PlaceMonthlyScheduleSummary::totalStayTime).reversed()
                        .thenComparing((PlaceMonthlyScheduleSummary s) -> s.placeId() == null ? 1 : 0)
                        .thenComparing(s -> s.placeId() == null ? Long.MAX_VALUE : s.placeId())
                        .thenComparing(PlaceMonthlyScheduleSummary::placeName, Comparator.nullsLast(String::compareTo))
                )
                .limit(3)
                .toList();

        List<Long> placeIds = top3Base.stream()
                .map(PlaceMonthlyScheduleSummary::placeId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        List<String> placeNames = top3Base.stream()
                .filter(s -> s.placeId() == null)
                .map(PlaceMonthlyScheduleSummary::placeName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList();

        List<Long> safePlaceIds = placeIds.isEmpty() ? List.of(-1L) : placeIds;
        List<String> safePlaceNames = placeNames.isEmpty() ? List.of("__NO_MATCH__") : placeNames;

        List<Object[]> expenseAggRows =
                analyticsRepository.findPlaceMonthlyExpenseAggForPlaces(userId, start, end, safePlaceIds, safePlaceNames);

        Map<String, PlaceExpenseAgg> expenseMap = expenseAggRows.stream()
                .collect(Collectors.toMap(
                        r -> (r[0] != null ? "ID-" + ((Number) r[0]).longValue() : "NAME-" + (String) r[1]),
                        r -> new PlaceExpenseAgg(
                                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                                r[3] != null ? ((Number) r[3]).doubleValue() : 0.0
                        ),
                        (a, b) -> a
                ));

        List<PlaceStatItem> top3 = top3Base.stream()
                .map(s -> {
                    String key = (s.placeId() != null ? "ID-" + s.placeId() : "NAME-" + s.placeName());
                    PlaceExpenseAgg exp = expenseMap.get(key);

                    long totalAmount = exp != null ? exp.totalAmount() : 0L;
                    double avgAmount = exp != null ? exp.avgAmount() : 0.0;

                    double avgStayMin = s.visitCount() > 0
                            ? Math.round((double) s.totalStayTime() / s.visitCount())
                            : 0.0;

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

        // DTO에서 totalVisitCount / totalStayMin 뺐다면 여기도 제거
        return new PlaceStatSummary(start, top3);
    }




    @Transactional(readOnly = true)
    public Page<PlaceStatItem> getPlaceStatList(Long userId, YearMonth yearMonth, int page, int size){
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        int offset = page * size;

        List<Object[]> scheduleMonthlySummary =
                analyticsRepository.findPlaceMonthlyStatsPaged(userId, start, end, size, offset);

        List<PlaceMonthlyScheduleSummary> scheduleStats = scheduleMonthlySummary.stream()
                .map(row -> new PlaceMonthlyScheduleSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        row[1] != null ? ((Number) row[1]).longValue() : null, // place_id
                        (String) row[2],                                       // place_name
                        row[3] != null ? ((Number) row[3]).intValue() : 0,     // visit_count
                        row[4] != null ? ((Number) row[4]).longValue() : 0L    // total_duration_min
                ))
                .toList();

        List<Long> placeIds = scheduleStats.stream()
                .map(PlaceMonthlyScheduleSummary::placeId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        List<String> placeNames = scheduleStats.stream()
                .filter(s -> s.placeId() == null)
                .map(PlaceMonthlyScheduleSummary::placeName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList();

        List<Long> safePlaceIds = placeIds.isEmpty() ? List.of(-1L) : placeIds;
        List<String> safePlaceNames = placeNames.isEmpty() ? List.of("__NO_MATCH__") : placeNames;

        List<Object[]> expenseRows =
                analyticsRepository.findPlaceMonthlyExpenseAggForPlaces(userId, start, end, safePlaceIds, safePlaceNames);

        Map<String, ExpenseAgg> expenseMap = expenseRows.stream()
                .collect(Collectors.toMap(
                        r -> (r[0] != null ? "ID-" + ((Number) r[0]).longValue() : "NAME-" + (String) r[1]),
                        r -> new ExpenseAgg(
                                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                                r[3] != null ? ((Number) r[3]).doubleValue() : 0.0
                        ),
                        (a,b) -> a
                ));

        // 5) 최종 items 생성 (방문수/체류시간은 schedule 기준, 지출은 expenseMap에서 붙이기)
        List<PlaceStatItem> items = scheduleStats.stream()
                .map(s -> {
                    String key = (s.placeId() != null) ? "ID-" + s.placeId() : "NAME-" + s.placeName();
                    ExpenseAgg exp = expenseMap.get(key);

                    long totalAmount = (exp != null) ? exp.totalAmount() : 0L;
                    double avgAmount = (exp != null) ? exp.avgAmount() : 0.0;

                    double avgStayMin = s.visitCount() > 0
                            ? Math.round((double) s.totalStayTime() / s.visitCount())
                            : 0.0;

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

        long total = analyticsRepository.countPlaceMonthlyStats(userId, start, end);
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(items, pageable, total);
    }

    private record PersonExpenseAgg(long sharedScheduleCount, long totalAmount, double avgAmount) {}

    @Transactional(readOnly = true)
    public Page<PeopleStatItem> getPeopleStatList(Long userId, YearMonth yearMonth, int page, int size) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        int offset = page * size;

        // 1) 만남(기준) 페이지 조회
        List<Object[]> scheduleSummary =
                analyticsRepository.findPersonMonthlyScheduleStatsPaged(userId, start, end, size, offset);

        List<PersonMonthlyScheduleSummary> scheduleStats = scheduleSummary.stream()
                .map(r -> new PersonMonthlyScheduleSummary(
                        ((Timestamp) r[0]).toLocalDateTime(),
                        r[1] != null ? ((Number) r[1]).longValue() : null,
                        (String) r[2],
                        ((Number) r[3]).intValue(),
                        r[4] != null ? ((Number) r[4]).longValue() : 0L
                ))
                .toList();

        // 2) 이 페이지에 포함된 사람들만 추출
        List<Long> personIds = scheduleStats.stream()
                .map(PersonMonthlyScheduleSummary::personId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        List<String> personNames = scheduleStats.stream()
                .filter(s -> s.personId() == null)
                .map(PersonMonthlyScheduleSummary::personName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList();

        List<Long> safePersonIds = personIds.isEmpty() ? List.of(-1L) : personIds;
        List<String> safePersonNames = personNames.isEmpty() ? List.of("__NO_MATCH__") : personNames;

        // 3) 이 페이지 사람들만 expense 집계 조회
        List<Object[]> expenseAggRows =
                analyticsRepository.findPersonMonthlyExpenseAggForPeople(userId, start, end, safePersonIds, safePersonNames);

        Map<String, PersonExpenseAgg> expenseMap = expenseAggRows.stream()
                .collect(Collectors.toMap(
                        r -> (r[0] != null ? "ID-" + ((Number) r[0]).longValue() : "NAME-" + (String) r[1]),
                        r -> new PersonExpenseAgg(
                                r[2] != null ? ((Number) r[2]).longValue() : 0L, // shared_schedule_count
                                r[3] != null ? ((Number) r[3]).longValue() : 0L, // total_amount
                                r[4] != null ? ((Number) r[4]).doubleValue() : 0.0 // avg_amount
                        ),
                        (a, b) -> a
                ));

        // 4) 응답 items 생성
        List<PeopleStatItem> items = scheduleStats.stream()
                .map(s -> {
                    String key = (s.personId() != null ? "ID-" + s.personId() : "NAME-" + s.personName());
                    PersonExpenseAgg exp = expenseMap.get(key);

                    long totalAmount = exp != null ? exp.totalAmount() : 0L;
                    double avgAmount = (exp != null && exp.sharedScheduleCount() > 0)
                            ? Math.round((double) totalAmount / exp.sharedScheduleCount())
                            : 0.0;

                    double avgDurationMin = s.meetCount() > 0
                            ? Math.round((double) s.totalDurationMin() / s.meetCount())
                            : 0.0;

                    return new PeopleStatItem(
                            s.personId(),
                            s.personName(),
                            s.meetCount(),
                            s.totalDurationMin(),
                            avgDurationMin,
                            totalAmount,
                            avgAmount
                    );
                })
                .toList();

        long total = analyticsRepository.countPersonMonthlyScheduleStats(userId, start, end);
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(items, pageable, total);
    }


    @Transactional(readOnly = true)
    public PeopleStatSummary getPeopleSummary(Long userId, YearMonth yearMonth){
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> scheduleMonthlySummary =
                analyticsRepository.findPersonMonthlyScheduleStats(userId, start, end);

        List<PersonMonthlyScheduleSummary> scheduleStats = scheduleMonthlySummary.stream()
                .map(row -> new PersonMonthlyScheduleSummary(
                        ((Timestamp) row[0]).toLocalDateTime(),
                        row[1] != null ? ((Number) row[1]).longValue() : null,
                        (String) row[2],
                        ((Number) row[3]).intValue(),
                        row[4] != null ? ((Number) row[4]).longValue() : 0L
                ))
                .toList();

        // Top3: meetCount desc -> duration desc -> (id/name)로 안정화
        List<PersonMonthlyScheduleSummary> top3Base = scheduleStats.stream()
                .sorted(Comparator
                        .comparingInt(PersonMonthlyScheduleSummary::meetCount).reversed()
                        .thenComparingLong(PersonMonthlyScheduleSummary::totalDurationMin).reversed()
                        .thenComparing((PersonMonthlyScheduleSummary s) -> s.personId() == null ? 1 : 0) // null은 뒤로
                        .thenComparing(s -> s.personId() == null ? Long.MAX_VALUE : s.personId())
                        .thenComparing(PersonMonthlyScheduleSummary::personName, Comparator.nullsLast(String::compareTo))
                )
                .limit(3)
                .toList();

        List<Long> personIds = top3Base.stream()
                .map(PersonMonthlyScheduleSummary::personId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        List<String> personNames = top3Base.stream()
                .filter(s -> s.personId() == null)
                .map(PersonMonthlyScheduleSummary::personName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList();

        List<Long> safePersonIds = personIds.isEmpty() ? List.of(-1L) : personIds;
        List<String> safePersonNames = personNames.isEmpty() ? List.of("__NO_MATCH__") : personNames;

        List<Object[]> expenseAggRows =
                analyticsRepository.findPersonMonthlyExpenseAggForPeople(userId, start, end, safePersonIds, safePersonNames);

        Map<String, PersonExpenseAgg> expenseMap = expenseAggRows.stream()
                .collect(Collectors.toMap(
                        r -> (r[0] != null ? "ID-" + ((Number) r[0]).longValue() : "NAME-" + (String) r[1]),
                        r -> new PersonExpenseAgg(
                                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                                r[3] != null ? ((Number) r[3]).longValue() : 0L,
                                r[4] != null ? ((Number) r[4]).doubleValue() : 0.0
                        ),
                        (a, b) -> a
                ));

        List<PeopleStatItem> top3 = top3Base.stream()
                .map(s -> {
                    String key = (s.personId() != null ? "ID-" + s.personId() : "NAME-" + s.personName());
                    PersonExpenseAgg exp = expenseMap.get(key);

                    long totalAmount = exp != null ? exp.totalAmount() : 0L;
                    double avgAmount = (exp != null && exp.sharedScheduleCount() > 0)
                            ? Math.round((double) totalAmount / exp.sharedScheduleCount())
                            : 0.0;

                    double avgDurationMin = s.meetCount() > 0
                            ? Math.round((double) s.totalDurationMin() / s.meetCount())
                            : 0.0;

                    return new PeopleStatItem(
                            s.personId(),
                            s.personName(),
                            s.meetCount(),
                            s.totalDurationMin(),
                            avgDurationMin,
                            totalAmount,
                            avgAmount
                    );
                })
                .toList();

        // DTO에서 totalMeetCount / totalDurationMin 뺐다면 여기도 제거
        return new PeopleStatSummary(start, top3);
    }

}
