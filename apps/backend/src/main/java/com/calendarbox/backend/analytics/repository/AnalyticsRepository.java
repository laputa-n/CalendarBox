package com.calendarbox.backend.analytics.repository;

import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Schedule, Long> {

    // 월별 친구 통계
    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at   AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
person_schedules AS (
    -- A. 나 말고 ACCEPTED 참여자
    SELECT DISTINCT
        e.month,
        s.schedule_id,
        sp.member_id AS person_id,
        COALESCE(m.name, sp.name) AS person_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at))/60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_participant sp ON sp.schedule_id = s.schedule_id
    LEFT JOIN member m ON m.member_id = sp.member_id
    WHERE sp.status = 'ACCEPTED'
      AND (sp.member_id IS NULL OR sp.member_id <> :memberId)

    UNION

    -- B. creator가 나가 아닌 경우
    SELECT DISTINCT
        e.month,
        s.schedule_id,
        s.created_by AS person_id,
        creator.name AS person_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at))/60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    LEFT JOIN member creator ON creator.member_id = s.created_by
    WHERE s.created_by <> :memberId
)
SELECT
    ps.month,
    ps.person_id,
    ps.person_name,
    COUNT(DISTINCT ps.schedule_id) AS meet_count,
    SUM(ps.duration_min)           AS total_duration_min
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month <  :endMonth
GROUP BY ps.month, ps.person_id, ps.person_name
ORDER BY ps.month, meet_count DESC NULLS LAST, total_duration_min DESC
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyScheduleStats(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth
    );

    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at   AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
person_schedules AS (
    -- A. 나 말고 ACCEPTED 참여자
    SELECT DISTINCT
        e.month,
        s.schedule_id,
        sp.member_id AS person_id,
        COALESCE(m.name, sp.name) AS person_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at))/60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_participant sp ON sp.schedule_id = s.schedule_id
    LEFT JOIN member m ON m.member_id = sp.member_id
    WHERE sp.status = 'ACCEPTED'
      AND (sp.member_id IS NULL OR sp.member_id <> :memberId)

    UNION

    -- B. creator가 나가 아닌 경우
    SELECT DISTINCT
        e.month,
        s.schedule_id,
        s.created_by AS person_id,
        creator.name AS person_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at))/60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    LEFT JOIN member creator ON creator.member_id = s.created_by
    WHERE s.created_by <> :memberId
)
SELECT
    ps.month,
    ps.person_id,
    ps.person_name,
    COUNT(DISTINCT ps.schedule_id) AS meet_count,
    SUM(ps.duration_min)           AS total_duration_min
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month <  :endMonth
GROUP BY ps.month, ps.person_id, ps.person_name
ORDER BY
    ps.month,
    meet_count DESC NULLS LAST,
    total_duration_min DESC,
    ps.person_id NULLS LAST,
    ps.person_name
LIMIT :size OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyScheduleStatsPaged(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT
        ms.schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at   AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) gs
),
person_schedules AS (
    SELECT DISTINCT
        e.month,
        sp.member_id AS person_id,
        COALESCE(m.name, sp.name) AS person_name
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_participant sp ON sp.schedule_id = s.schedule_id
    LEFT JOIN member m ON m.member_id = sp.member_id
    WHERE sp.status = 'ACCEPTED'
      AND (sp.member_id IS NULL OR sp.member_id <> :memberId)

    UNION

    SELECT DISTINCT
        e.month,
        s.created_by AS person_id,
        creator.name AS person_name
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    LEFT JOIN member creator ON creator.member_id = s.created_by
    WHERE s.created_by <> :memberId
)
SELECT COUNT(DISTINCT COALESCE(ps.person_id::text, ps.person_name))
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month <  :endMonth
""", nativeQuery = true)
    long countPersonMonthlyScheduleStats(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth
    );

    //월별 장소 통계
    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
place_schedules AS (
    SELECT DISTINCT
        e.month,
        s.schedule_id AS schedule_id,
        COALESCE(p.place_id, NULL) AS place_id,
        COALESCE(p.title, sp2.name) AS place_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_place sp2 ON s.schedule_id = sp2.schedule_id
    LEFT JOIN place p ON sp2.place_id = p.place_id
)
SELECT
    ps.month,
    ps.place_id,
    ps.place_name,
    COUNT(DISTINCT ps.schedule_id) AS visit_count,
    SUM(ps.duration_min) AS total_duration_min
FROM place_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
GROUP BY ps.month, ps.place_id, ps.place_name
ORDER BY ps.month, visit_count DESC NULLS LAST, total_duration_min DESC
""", nativeQuery = true)
    List<Object[]> findPlaceMonthlyStats(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth
    );

    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
place_schedules AS (
    SELECT DISTINCT
        e.month,
        s.schedule_id AS schedule_id,
        COALESCE(p.place_id, NULL) AS place_id,
        COALESCE(p.title, sp2.name) AS place_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_place sp2 ON s.schedule_id = sp2.schedule_id
    LEFT JOIN place p ON sp2.place_id = p.place_id
)
SELECT
    ps.month,
    ps.place_id,
    ps.place_name,
    COUNT(DISTINCT ps.schedule_id) AS visit_count,
    SUM(ps.duration_min) AS total_duration_min
FROM place_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
GROUP BY ps.month, ps.place_id, ps.place_name
ORDER BY
    ps.month,
    visit_count DESC NULLS LAST,
    total_duration_min DESC,
    ps.place_id NULLS LAST,
    ps.place_name
LIMIT :size OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findPlaceMonthlyStatsPaged(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
place_schedules AS (
    SELECT DISTINCT
        e.month,
        s.schedule_id AS schedule_id,
        COALESCE(p.place_id, NULL) AS place_id,
        COALESCE(p.title, sp2.name) AS place_name,
        EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_place sp2 ON s.schedule_id = sp2.schedule_id
    LEFT JOIN place p ON sp2.place_id = p.place_id
)
SELECT
    COUNT(DISTINCT COALESCE(ps.place_id::text, ps.place_name))
FROM place_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
""", nativeQuery = true)
    long countPlaceMonthlyStats(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth
    );

    // 요일 + 시간별 스케줄 통계
    @Query(value = """
    WITH my_schedules AS(
        SELECT s.schedule_id, s.start_at, s.end_at
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.schedule_id, s.start_at, s.end_at
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.schedule_id
        WHERE sp.member_id = :memberId
            AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.schedule_id AS schedule_id,
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
    List<Object[]> findDayHourScheduleDistribution(
            @Param("memberId") Long memberId
    );


    //월별 일정 증가 추이
    @Query(value = """
    WITH my_schedules AS (
        SELECT s.schedule_id, s.start_at, s.end_at
        FROM schedule s
        WHERE s.created_by = :memberId
        UNION
        SELECT s.schedule_id, s.start_at, s.end_at
        FROM schedule_participant sp
        JOIN schedule s ON sp.schedule_id = s.schedule_id
        WHERE sp.member_id = :memberId
          AND sp.status = 'ACCEPTED'
    ),
    expanded AS (
        SELECT 
            ms.schedule_id AS schedule_id,
            date_trunc('month', gs) AS month
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
    List<Object[]> findMonthlyScheduleTrend(
            @Param("memberId") Long memberId
    );

    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at   AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
place_schedules AS (
    SELECT DISTINCT
        e.month,
        s.schedule_id AS schedule_id,
        p.place_id AS place_id,
        COALESCE(p.title, sp2.name) AS place_name
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_place sp2 ON s.schedule_id = sp2.schedule_id
    LEFT JOIN place p ON sp2.place_id = p.place_id
),
place_expenses AS (
    SELECT
        ps.month,
        ps.place_id,
        ps.place_name,
        ps.schedule_id,
        COALESCE(SUM(e2.amount), 0) AS total_amount
    FROM place_schedules ps
    LEFT JOIN expense e2 ON e2.schedule_id = ps.schedule_id
    GROUP BY ps.month, ps.place_id, ps.place_name, ps.schedule_id
)
SELECT
    pe.place_id,
    pe.place_name,
    SUM(pe.total_amount) AS total_amount,
    AVG(pe.total_amount) AS avg_amount
FROM place_expenses pe
WHERE pe.month >= :startMonth
  AND pe.month <  :endMonth
  AND (
        (pe.place_id IS NOT NULL AND pe.place_id IN (:placeIds))
     OR (pe.place_id IS NULL AND pe.place_name IN (:placeNames))
  )
GROUP BY pe.place_id, pe.place_name
""", nativeQuery = true)
    List<Object[]> findPlaceMonthlyExpenseAggForPlaces(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth,
            @Param("placeIds") List<Long> placeIds,
            @Param("placeNames") List<String> placeNames
    );

    @Query(value = """
WITH my_schedules AS (
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule s
    WHERE s.created_by = :memberId
    UNION
    SELECT s.schedule_id, s.start_at, s.end_at, s.created_by
    FROM schedule_participant sp
    JOIN schedule s ON sp.schedule_id = s.schedule_id
    WHERE sp.member_id = :memberId
      AND sp.status = 'ACCEPTED'
),
expanded AS (
    SELECT 
        ms.schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at   AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
schedule_expense AS (
    SELECT schedule_id, SUM(amount) AS schedule_total_amount
    FROM expense
    GROUP BY schedule_id
),
person_schedules AS (
    -- A) 나 말고 ACCEPTED 참여자
    SELECT DISTINCT
        e.month,
        s.schedule_id,
        sp.member_id AS person_id,
        COALESCE(m.name, sp.name) AS person_name,
        COALESCE(se.schedule_total_amount, 0) AS schedule_total_amount
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    JOIN schedule_participant sp ON sp.schedule_id = s.schedule_id
    LEFT JOIN member m ON m.member_id = sp.member_id
    LEFT JOIN schedule_expense se ON se.schedule_id = s.schedule_id
    WHERE sp.status = 'ACCEPTED'
      AND (sp.member_id IS NULL OR sp.member_id <> :memberId)

    UNION

    -- B) creator가 나가 아닌 경우
    SELECT DISTINCT
        e.month,
        s.schedule_id,
        s.created_by AS person_id,
        creator.name AS person_name,
        COALESCE(se.schedule_total_amount, 0) AS schedule_total_amount
    FROM expanded e
    JOIN schedule s ON s.schedule_id = e.schedule_id
    LEFT JOIN member creator ON creator.member_id = s.created_by
    LEFT JOIN schedule_expense se ON se.schedule_id = s.schedule_id
    WHERE s.created_by <> :memberId
)
SELECT
    ps.person_id,
    ps.person_name,
    COUNT(DISTINCT ps.schedule_id) AS shared_schedule_count,
    SUM(ps.schedule_total_amount)  AS total_amount,
    AVG(ps.schedule_total_amount)  AS avg_amount
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month <  :endMonth
  AND (
        (ps.person_id IS NOT NULL AND ps.person_id IN (:personIds))
     OR (ps.person_id IS NULL AND ps.person_name IN (:personNames))
  )
GROUP BY ps.person_id, ps.person_name
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyExpenseAggForPeople(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth,
            @Param("personIds") List<Long> personIds,
            @Param("personNames") List<String> personNames
    );

}
