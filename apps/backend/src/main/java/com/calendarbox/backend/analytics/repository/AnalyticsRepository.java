package com.calendarbox.backend.analytics.repository;

import com.calendarbox.backend.analytics.dto.request.PlaceSummary;
import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Schedule, Long> {

//    @Query(value = """
//        SELECT
//            s.schedule_id AS scheduleId,
//            s.title AS title,
//            p.title AS placeName,
//            EXTRACT(HOUR FROM s.start_at) AS hour,
//            EXTRACT(EPOCH FROM (s.end_at - s.start_at))/60 AS durationMin,
//            COALESCE(e.amount, 0) AS amount
//        FROM schedule s
//        LEFT JOIN schedule_place sp ON s.schedule_id = sp.schedule_id
//        LEFT JOIN place p ON sp.place_id = p.place_id
//        LEFT JOIN expense e ON e.schedule_id = s.schedule_id
//        WHERE s.created_by = :memberId
//        """,
//            nativeQuery = true)
//    List<Object[]> findScheduleSummaries(Long memberId);
//
//    @Query(value = """
//        SELECT
//            p.title AS placeName, COUNT(*) AS visitCount
//        FROM schedule_place sp
//        JOIN place p ON sp.place_id = p.place_id
//        JOIN schedule s ON s.schedule_id = sp.schedule_id
//        WHERE s.created_by = :memberId
//        GROUP BY p.title
//        """,
//            nativeQuery = true)
//    List<Object[]> findPlaceStats(Long memberId);
//
//    @Query(value = """
//        SELECT
//            date_trunc('month', s.start_at) AS month, COUNT(*) AS count
//        FROM schedule s
//        WHERE s.created_by = :memberId
//        GROUP BY date_trunc('month', s.start_at)
//        ORDER BY date_trunc('month', s.start_at)
//        """,
//            nativeQuery = true)
//    List<Object[]> findMonthlyTrend(Long memberId);
//
//    @Query("""
//    SELECT new com.calendarbox.backend.analytics.dto.request.PlaceSummary(
//        p.title,
//        date_trunc('month', s.startAt),
//        COUNT(*)
//    )
//    FROM SchedulePlace sp
//    JOIN Place p ON sp.place.id = p.id
//    JOIN Schedule s ON s.id = sp.schedule.id
//    WHERE s.createdBy.id = :memberId
//    GROUP BY p.title, date_trunc('month', s.startAt)
//    ORDER BY date_trunc('month', s.startAt)
//""")
//    List<PlaceSummary> findMonthlyPlaceStats(Long memberId);

    //================================================================================

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
            ms.schedule_id AS schedule_id,
            date_trunc('month', gs) AS month
        FROM my_schedules ms,
             generate_series(
                 date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
                 date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
                 interval '1 month'
             ) AS gs
    ),
    person_schedules AS (
     -- A. 나 말고, ACCEPTED 된 참여자들
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         sp.member_id AS person_id,
         sp.name      AS person_name,
         EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     JOIN schedule_participant sp ON s.schedule_id = sp.schedule_id
     WHERE sp.status = 'ACCEPTED'
       AND (sp.member_id IS NULL OR sp.member_id <> :memberId)
 
     UNION
 
     -- B. 일정 만든 사람(creator)이 나가 아닌 경우
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         s.created_by     AS person_id,
         creator.name     AS person_name,
         EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
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
    SUM(ps.duration_min) AS total_duration_min
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
GROUP BY ps.month, ps.person_id, ps.person_name
ORDER BY ps.month, meet_count DESC NULLS LAST
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyScheduleStats(@Param("memberId") Long memberId, @Param("startMonth") LocalDateTime startMonth, @Param("endMonth") LocalDateTime endMonth);

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
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
            person_schedules AS (
     -- A. 나 말고, ACCEPTED 된 참여자들
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         sp.member_id AS person_id,
         sp.name      AS person_name,
         EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     JOIN schedule_participant sp ON s.schedule_id = sp.schedule_id
     WHERE sp.status = 'ACCEPTED'
       AND (sp.member_id IS NULL OR sp.member_id <> :memberId)
 
     UNION
 
     -- B. 일정 만든 사람(creator)이 나가 아닌 경우
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         s.created_by     AS person_id,
         creator.name     AS person_name,
         EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
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
    SUM(ps.duration_min) AS total_duration_min
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
GROUP BY ps.month, ps.person_id, ps.person_name
ORDER BY ps.month, meet_count DESC NULLS LAST
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
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
            person_schedules AS (
     -- A. 나 말고, ACCEPTED 된 참여자들
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         sp.member_id AS person_id,
         sp.name      AS person_name,
         EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     JOIN schedule_participant sp ON s.schedule_id = sp.schedule_id
     WHERE sp.status = 'ACCEPTED'
       AND (sp.member_id IS NULL OR sp.member_id <> :memberId)
 
     UNION
 
     -- B. 일정 만든 사람(creator)이 나가 아닌 경우
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         s.created_by     AS person_id,
         creator.name     AS person_name,
         EXTRACT(EPOCH FROM (s.end_at - s.start_at)) / 60 AS duration_min
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     LEFT JOIN member creator ON creator.member_id = s.created_by
     WHERE s.created_by <> :memberId
 )
SELECT COUNT(DISTINCT COALESCE(ps.person_id::text, ps.person_name))
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
""", nativeQuery = true)
    long countPersonMonthlyScheduleStats(
            @Param("memberId") Long memberId,
            @Param("startMonth") LocalDateTime startMonth,
            @Param("endMonth") LocalDateTime endMonth
    );

    //사람별 지출 통계
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
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
            person_schedules AS (
     -- A. 나 말고, ACCEPTED 된 참여자들
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         sp.member_id AS person_id,
         sp.name      AS person_name,
         e2.amount    AS amount
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     JOIN schedule_participant sp ON s.schedule_id = sp.schedule_id
     LEFT JOIN expense e2 ON e2.schedule_id = s.schedule_id
     WHERE sp.status = 'ACCEPTED'
       AND (sp.member_id IS NULL OR sp.member_id <> :memberId)
 
     UNION
 
     -- B. 일정 만든 사람(creator)이 나가 아닌 경우
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         s.created_by   AS person_id,
         creator.name   AS person_name,
         e2.amount      AS amount
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     LEFT JOIN member creator ON creator.member_id = s.created_by
     LEFT JOIN expense e2 ON e2.schedule_id = s.schedule_id
     WHERE s.created_by <> :memberId
 )
SELECT 
    ps.month,
    ps.person_id,
    ps.person_name,
    COUNT(DISTINCT ps.schedule_id) AS shared_schedule_count,
    SUM(ps.amount) AS total_amount,
    AVG(ps.amount) AS avg_amount
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
GROUP BY ps.month, ps.person_id, ps.person_name
ORDER BY ps.month, total_amount DESC NULLS LAST, avg_amount DESC NULLS LAST
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyExpenseStats(
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
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
            person_schedules AS (
     -- A. 나 말고, ACCEPTED 된 참여자들
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         sp.member_id AS person_id,
         sp.name      AS person_name,
         e2.amount    AS amount
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     JOIN schedule_participant sp ON s.schedule_id = sp.schedule_id
     LEFT JOIN expense e2 ON e2.schedule_id = s.schedule_id
     WHERE sp.status = 'ACCEPTED'
       AND (sp.member_id IS NULL OR sp.member_id <> :memberId)
 
     UNION
 
     -- B. 일정 만든 사람(creator)이 나가 아닌 경우
     SELECT DISTINCT
         e.month,
         s.schedule_id,
         s.created_by   AS person_id,
         creator.name   AS person_name,
         e2.amount      AS amount
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     LEFT JOIN member creator ON creator.member_id = s.created_by
     LEFT JOIN expense e2 ON e2.schedule_id = s.schedule_id
     WHERE s.created_by <> :memberId
 )
SELECT 
    ps.month,
    ps.person_id,
    ps.person_name,
    COUNT(DISTINCT ps.schedule_id) AS shared_schedule_count,
    SUM(ps.amount) AS total_amount,
    AVG(ps.amount) AS avg_amount
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
GROUP BY ps.month, ps.person_id, ps.person_name
ORDER BY ps.month, total_amount DESC NULLS LAST, avg_amount DESC NULLS LAST
LIMIT :size OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findPersonMonthlyExpenseStatsPaged(
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
        ms.schedule_id AS schedule_id,
        date_trunc('month', gs) AS month
    FROM my_schedules ms,
         generate_series(
             date_trunc('month', ms.start_at AT TIME ZONE 'Asia/Seoul'),
             date_trunc('month', ms.end_at AT TIME ZONE 'Asia/Seoul'),
             interval '1 month'
         ) AS gs
),
            person_schedules AS (
     -- A. 나 말고, ACCEPTED 된 참여자들
     SELECT DISTINCT
         e.month,
         sp.member_id AS person_id,
         sp.name      AS person_name
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     JOIN schedule_participant sp ON s.schedule_id = sp.schedule_id
     LEFT JOIN expense e2 ON e2.schedule_id = s.schedule_id
     WHERE sp.status = 'ACCEPTED'
       AND (sp.member_id IS NULL OR sp.member_id <> :memberId)
 
     UNION
 
     -- B. 일정 만든 사람(creator)이 나가 아닌 경우
     SELECT DISTINCT
         e.month,
         s.created_by   AS person_id,
         creator.name   AS person_name
     FROM expanded e
     JOIN schedule s ON s.schedule_id = e.schedule_id
     LEFT JOIN member creator ON creator.member_id = s.created_by
     LEFT JOIN expense e2 ON e2.schedule_id = s.schedule_id
     WHERE s.created_by <> :memberId
 )
SELECT COUNT(DISTINCT COALESCE(ps.person_id::text, ps.person_name))
FROM person_schedules ps
WHERE ps.month >= :startMonth
  AND ps.month < :endMonth
""", nativeQuery = true)
    long countPersonMonthlyExpenseStats(
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
ORDER BY ps.month, visit_count DESC NULLS LAST
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
ORDER BY ps.month, visit_count DESC NULLS LAST
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


    //장소별 지출 통계
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
),
place_expenses AS (
    SELECT
        ps.month,
        ps.place_id,
        ps.place_name,
        ps.schedule_id,
        ps.duration_min,
        SUM(e2.amount) AS total_amount
    FROM place_schedules ps
    LEFT JOIN expense e2 ON e2.schedule_id = ps.schedule_id
    GROUP BY ps.month, ps.place_id, ps.place_name, ps.schedule_id, ps.duration_min
)
SELECT 
    pe.month,
    pe.place_id,
    pe.place_name,
    COUNT(DISTINCT pe.schedule_id) AS visit_count,
    SUM(pe.duration_min) AS total_duration_min,
    SUM(pe.total_amount) AS total_amount,
    AVG(pe.total_amount) AS avg_amount
FROM place_expenses pe
WHERE pe.month >= :startMonth
  AND pe.month < :endMonth
GROUP BY pe.month, pe.place_id, pe.place_name
ORDER BY pe.month, total_amount DESC NULLS LAST, avg_amount DESC NULLS LAST
""", nativeQuery = true)
    List<Object[]> findPlaceMonthlyExpenseStats(
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
),
place_expenses AS (
    SELECT
        ps.month,
        ps.place_id,
        ps.place_name,
        ps.schedule_id,
        ps.duration_min,
        SUM(e2.amount) AS total_amount
    FROM place_schedules ps
    LEFT JOIN expense e2 ON e2.schedule_id = ps.schedule_id
    GROUP BY ps.month, ps.place_id, ps.place_name, ps.schedule_id, ps.duration_min
)
SELECT 
    pe.month,
    pe.place_id,
    pe.place_name,
    COUNT(DISTINCT pe.schedule_id) AS visit_count,
    SUM(pe.duration_min) AS total_duration_min,
    SUM(pe.total_amount) AS total_amount,
    AVG(pe.total_amount) AS avg_amount
FROM place_expenses pe
WHERE pe.month >= :startMonth
  AND pe.month < :endMonth
GROUP BY pe.month, pe.place_id, pe.place_name
ORDER BY pe.month, total_amount DESC NULLS LAST, avg_amount DESC NULLS LAST
LIMIT :size OFFSET :offset
""", nativeQuery = true)
    List<Object[]> findPlaceMonthlyExpenseStatsPaged(
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
),
place_expenses AS (
    SELECT
        ps.month,
        ps.place_id,
        ps.place_name,
        ps.schedule_id,
        ps.duration_min,
        SUM(e2.amount) AS total_amount
    FROM place_schedules ps
    LEFT JOIN expense e2 ON e2.schedule_id = ps.schedule_id
    GROUP BY ps.month, ps.place_id, ps.place_name, ps.schedule_id, ps.duration_min
)
SELECT 
    COUNT(DISTINCT COALESCE(pe.place_id::text, pe.place_name))
FROM place_expenses pe
WHERE pe.month >= :startMonth
  AND pe.month < :endMonth
""", nativeQuery = true)
    long countPlaceMonthlyExpenseStats(
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
}
