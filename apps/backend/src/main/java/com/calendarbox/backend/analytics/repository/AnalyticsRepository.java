package com.calendarbox.backend.analytics.repository;

import com.calendarbox.backend.analytics.dto.request.PlaceSummary;
import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Schedule, Long> {

    @Query(value = """
        SELECT 
            s.schedule_id AS scheduleId,
            s.title AS title,
            p.title AS placeName,
            EXTRACT(HOUR FROM s.start_at) AS hour,
            EXTRACT(EPOCH FROM (s.end_at - s.start_at))/60 AS durationMin,
            COALESCE(e.amount, 0) AS amount
        FROM schedule s
        LEFT JOIN schedule_place sp ON s.schedule_id = sp.schedule_id
        LEFT JOIN place p ON sp.place_id = p.place_id
        LEFT JOIN expense e ON e.schedule_id = s.schedule_id
        WHERE s.created_by = :memberId
        """,
            nativeQuery = true)
    List<Object[]> findScheduleSummaries(Long memberId);

    @Query(value = """
        SELECT 
            p.title AS placeName, COUNT(*) AS visitCount
        FROM schedule_place sp
        JOIN place p ON sp.place_id = p.place_id
        JOIN schedule s ON s.schedule_id = sp.schedule_id
        WHERE s.created_by = :memberId
        GROUP BY p.title
        """,
            nativeQuery = true)
    List<Object[]> findPlaceStats(Long memberId);

    @Query(value = """
        SELECT 
            date_trunc('month', s.start_at) AS month, COUNT(*) AS count
        FROM schedule s
        WHERE s.created_by = :memberId
        GROUP BY date_trunc('month', s.start_at)
        ORDER BY date_trunc('month', s.start_at)
        """,
            nativeQuery = true)
    List<Object[]> findMonthlyTrend(Long memberId);

    @Query("""
    SELECT new com.calendarbox.backend.analytics.dto.request.PlaceSummary(
        p.title,
        date_trunc('month', s.startAt),
        COUNT(*)
    )
    FROM SchedulePlace sp
    JOIN Place p ON sp.place.id = p.id
    JOIN Schedule s ON s.id = sp.schedule.id
    WHERE s.createdBy.id = :memberId
    GROUP BY p.title, date_trunc('month', s.startAt)
    ORDER BY date_trunc('month', s.startAt)
""")
    List<PlaceSummary> findMonthlyPlaceStats(Long memberId);

    //================================================================================

    // 월별 친구 통계
    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT
        e.month,
        COALESCE(sp.member_id, s.created_by) AS person_id,
        COALESCE(sp.name, creator.name) AS person_name,
        COUNT(DISTINCT s.id) AS meet_count,
        SUM(EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60) AS total_duration_min
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_participant sp ON s.id = sp.schedule_id
    LEFT JOIN member creator ON creator.id = s.created_by
    WHERE (
        (sp.member_id IS NULL OR sp.member_id <> :memberId)
        AND s.created_by <> :memberId
    )
        AND e.month >= :startMonth
        AND e.month < :endMonth
    GROUP BY e.month, person_id, person_name
    ORDER BY e.month, meet_count DESC
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyScheduleStats(Long memberId, LocalDateTime startMonth, LocalDateTime endMonth);

    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT
        e.month,
        COALESCE(sp.member_id, s.created_by) AS person_id,
        COALESCE(sp.name, creator.name) AS person_name,
        COUNT(DISTINCT s.id) AS meet_count,
        SUM(EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60) AS total_duration_min
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_participant sp ON s.id = sp.schedule_id
    LEFT JOIN member creator ON creator.id = s.created_by
    WHERE (
        (sp.member_id IS NULL OR sp.member_id <> :memberId)
        AND s.created_by <> :memberId
    )
        AND e.month >= :startMonth
        AND e.month < :endMonth
    GROUP BY e.month, person_id, person_name
    ORDER BY e.month, meet_count DESC
    LIMIT :size OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyScheduleStatsPaged(Long memberId, LocalDateTime startMonth, LocalDateTime endMonth,int size, int offset);

    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT
        SELECT COUNT(DISTINCT COALESCE(sp.member_id::text, s.created_by::text, sp.name))
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_participant sp ON s.id = sp.schedule_id
    LEFT JOIN member creator ON creator.id = s.created_by
    WHERE (
        (sp.member_id IS NULL OR sp.member_id <> :memberId)
        AND s.created_by <> :memberId
    )
        AND e.month >= :startMonth
        AND e.month < :endMonth
""", nativeQuery = true)
    long countPersonMonthlyScheduleStats(Long memberId, LocalDateTime startMonth, LocalDateTime endMonth);

    //사람별 지출 통계
    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT 
        e.month,
        COALESCE(sp.member_id, s.created_by) AS person_id,
        COALESCE(sp.name, creator.name) AS person_name,
        SUM(e2.amount) AS total_amount,
        AVG(e2.amount) AS avg_amount,
        COUNT(DISTINCT s.id) AS shared_schedule_count
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_participant sp ON s.id = sp.schedule_id
    LEFT JOIN member creator ON creator.id = s.created_by
    LEFT JOIN expense e2 ON e2.schedule_id = s.id
    WHERE (
        (sp.member_id IS NULL OR sp.member_id <> :memberId)
        AND s.created_by <> :memberId
    )
        AND e.month >= :startMonth
        AND e.month < :endMonth
    GROUP BY e.month, person_id, person_name
    ORDER BY e.month, total_amount DESC, avg_amount DESC
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyExpenseStats(Long memberId, LocalDateTime startMonth, LocalDateTime endMonth);

    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT 
        e.month,
        COALESCE(sp.member_id, s.created_by) AS person_id,
        COALESCE(sp.name, creator.name) AS person_name,
        SUM(e2.amount) AS total_amount,
        AVG(e2.amount) AS avg_amount,
        COUNT(DISTINCT s.id) AS shared_schedule_count
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_participant sp ON s.id = sp.schedule_id
    LEFT JOIN member creator ON creator.id = s.created_by
    LEFT JOIN expense e2 ON e2.schedule_id = s.id
    WHERE (
        (sp.member_id IS NULL OR sp.member_id <> :memberId)
        AND s.created_by <> :memberId
    )
      AND e.month >= :startMonth
      AND e.month < :endMonth
    GROUP BY e.month, person_id, person_name
    ORDER BY e.month, total_amount DESC, avg_amount DESC
    LIMIT :size OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyExpenseStatsPaged(Long memberId, LocalDateTime startMonth, LocalDateTime endMonth, int size, int offset);

    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at, s.created_by
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT COUNT(DISTINCT COALESCE(sp.member_id::text, s.created_by::text,sp.name))
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_participant sp ON s.id = sp.schedule_id
    LEFT JOIN member creator ON creator.id = s.created_by
    LEFT JOIN expense e2 ON e2.schedule_id = s.id
    WHERE (
        (sp.member_id IS NULL OR sp.member_id <> :memberId)
        AND s.created_by <> :memberId
    )
      AND e.month >= :startMonth
      AND e.month < :endMonth
""", nativeQuery = true)
    long countPersonMonthlyExpenseStats(Long memberId, LocalDateTime startMonth, LocalDateTime endMonth);



    //월별 장소 통계
    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT
        e.month,
        COALESCE(p.place_id, NULL) AS place_id,
        COALESCE(p.title, sp2.name) AS place_name,
        COUNT(DISTINCT e.schedule_id) AS visit_count,
        SUM(EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60) AS total_duration_min
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_place sp2 ON s.id = sp2.schedule_id
    LEFT JOIN place p ON sp2.place_id = p.place_id
    GROUP BY e.month, place_id, place_name
    ORDER BY e.month, visit_count DESC
""", nativeQuery = true)
    List<Object[]> findPlaceMonthlyStats(Long memberId);

    //장소별 지출 통계
    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT 
        e.month,
        COALESCE(p.place_id, NULL) AS place_id,
        COALESCE(p.title, sp2.name) AS place_name,
        SUM(e2.amount) AS total_amount,
        AVG(e2.amount) AS avg_amount,
        COUNT(DISTINCT s.id) AS visit_count,
        SUM(EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60) AS total_duration_min
    FROM expanded e
    JOIN schedule s ON s.id = e.schedule_id
    LEFT JOIN schedule_place sp2 ON s.id = sp2.schedule_id
    LEFT JOIN place p ON sp2.place_id = p.place_id
    LEFT JOIN expense e2 ON e2.schedule_id = s.id
    GROUP BY e.month, place_id, place_name
    ORDER BY e.month, total_amount DESC, avg_amount DESC
""", nativeQuery = true)
    List<Object[]> findPlaceExpenseStats(Long memberId);


    // 요일 + 시간별 스케줄 통계
    @Query(value = """
    WITH my_schedules AS(
        SELECT s.id
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT sp.schedule_id
        FROM schedule_participant sp
        WHERE sp.member_id = :memberId
            AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            EXTRACT(DOW FROM gs) AS day_of_week,
            EXTRACT(HOUR FROM gs) AS hour_of_day
        FROM my_schedules ms,
            generate_series(
                date_trunc('hour', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                date_trunc('hour', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                interval '1 hour'
            ) AS gs
    )
    SELECT
        day_of_week,
        hour_of_day,
        COUNT(DISTINCT schedule_id) AS schedule_count
    FROM expanded
    GROUP BY day_of_week, hour_of_day
    ORDER BY day_of_week, hour_of_day
    """, nativeQuery = true
    )
    List<Object[]> findDayHourScheduleDistribution(Long memberId);


    //월별 일정 증가 추이
    @Query(value = """
    WITH my_schedules AS (
        SELECT s.id, s.start_at, s.end_at
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.id, s.start_at, s.end_at
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.id AS schedule_id,
            date_trunc('month', gs)::date AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    )
    SELECT
        month,
        COUNT(DISTINCT schedule_id) AS schedule_count
    FROM expanded
    GROUP BY month
    ORDER BY month;
""", nativeQuery = true)
    List<Object[]> findMonthlyScheduleTrend(Long memberId);
}
