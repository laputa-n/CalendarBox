package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query(
            value = """
      SELECT s
      FROM Schedule s
      WHERE s.calendar.id IN :calendarIds
        AND s.startAt < :to
        AND s.endAt   > :from
      """,
            countQuery = """
      SELECT count(s)
      FROM Schedule s
      WHERE s.calendar.id IN :calendarIds
        AND s.startAt < :to
        AND s.endAt   > :from
      """
    )
    Page<Schedule> findAllOverlapping(@Param("calendarIds") Collection<Long> calendarIds,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to,
                                      Pageable pageable);

    @Query(
            value = """
      SELECT s
      FROM Schedule s
      WHERE s.calendar.id IN :calendarIds
        AND s.endAt > :from
      """,
            countQuery = """
      SELECT count(s)
      FROM Schedule s
      WHERE s.calendar.id IN :calendarIds
        AND s.endAt > :from
      """
    )
    Page<Schedule> findAllFrom(@Param("calendarIds") Collection<Long> calendarIds,
                               @Param("from") Instant from,
                               Pageable pageable);

    @Query(
            value = """
      SELECT s
      FROM Schedule s
      WHERE s.calendar.id IN :calendarIds
        AND s.startAt < :to
      """,
            countQuery = """
      SELECT count(s)
      FROM Schedule s
      WHERE s.calendar.id IN :calendarIds
        AND s.startAt < :to
      """
    )
    Page<Schedule> findAllUntil(@Param("calendarIds") Collection<Long> calendarIds,
                                @Param("to") Instant to,
                                Pageable pageable);

    @EntityGraph(attributePaths = "calendar")
    @Query(
            value = """
    select s
    from Schedule s
    where s.calendar.id in :calendarIds
      and (
        lower(s.title) like lower(concat('%', :q, '%'))
        or lower(s.memo)  like lower(concat('%', :q, '%'))
        or exists (select 1 from ScheduleParticipant p where p.schedule = s and lower(p.name) like lower(concat('%', :q, '%')))
        or exists (select 1 from ScheduleTodo t        where t.schedule = s and lower(t.content) like lower(concat('%', :q, '%')))
        or exists (select 1 from SchedulePlace sp      where sp.schedule = s and lower(sp.name) like lower(concat('%', :q, '%')))
        or exists (select 1 from ScheduleLink l        where l.schedule = s and lower(l.label) like lower(concat('%', :q, '%')))
        or exists (select 1 from Attachment a          where a.schedule = s and lower(a.originalName) like lower(concat('%', :q, '%')))
      )
  """,
            countQuery = """
    select count(s)
    from Schedule s
    where s.calendar.id in :calendarIds
      and (
        lower(s.title) like lower(concat('%', :q, '%'))
        or lower(s.memo)  like lower(concat('%', :q, '%'))
        or exists (select 1 from ScheduleParticipant p where p.schedule = s and lower(p.name) like lower(concat('%', :q, '%')))
        or exists (select 1 from ScheduleTodo t        where t.schedule = s and lower(t.content) like lower(concat('%', :q, '%')))
        or exists (select 1 from SchedulePlace sp      where sp.schedule = s and lower(sp.name) like lower(concat('%', :q, '%')))
        or exists (select 1 from ScheduleLink l        where l.schedule = s and lower(l.label) like lower(concat('%', :q, '%')))
        or exists (select 1 from Attachment a          where a.schedule = s and lower(a.originalName) like lower(concat('%', :q, '%')))
      )
  """
    )
    Page<Schedule> searchByKeyword(@Param("calendarIds") Collection<Long> calendarIds,
                                         @Param("q") String q,
                                         Pageable pageable);

    // (A) 단발 일정(기간 겹침)
    @Query("""
        select s
        from Schedule s
        where s.recurrence is null
          and s.calendar.id in :calIds
          and s.startAt < :toUtc
          and s.endAt   > :fromUtc
    """)
    List<Schedule> findNoRecurByCalendarIds(
            @Param("calIds") List<Long> calIds,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc")   Instant toUtc
    );

    @Query("""
        select distinct s
        from Schedule s
        join fetch s.recurrence r
        left join fetch r.exceptions e
        where s.calendar.id in :calIds
          and s.recurrence is not null
          and (r.until is null or r.until >= :fromUtc)
    """)
    List<Schedule> findRecurringWithExceptionsByCalendarIds(
            @Param("calIds") List<Long> calIds,
            @Param("fromUtc") Instant fromUtc
    );

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE schedule
           SET embedding_status = 'RUNNING', 
                updated_at = now(),
                embedding_dirty = false
         WHERE schedule_id = :id
           AND embedding_status = 'QUEUED'
        """, nativeQuery = true)
    int markEmbeddingRunningIfQueued(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
UPDATE schedule
   SET embedding_dirty = true,
       embedding_status = 'QUEUED',
       embedding_last_error = null
 WHERE schedule_id = :id
   AND embedding_status IN ('SYNCED', 'FAILED')
""", nativeQuery = true)
    int markEmbeddingQueuedIfSyncedOrFailed(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
UPDATE schedule
   SET embedding_dirty = true,
       embedding_last_error = null
 WHERE schedule_id = :id
   AND embedding_status = 'RUNNING'
""", nativeQuery = true)
    int markEmbeddingDirtyIfRunning(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
WITH candidates AS (
    SELECT s.schedule_id
    FROM schedule s
    LEFT JOIN schedule_embedding e ON e.schedule_id = s.schedule_id
    WHERE s.embedding_status IN ('SYNCED','FAILED')
      AND s.embedding_fail_count < :maxFailCount
      AND (
            e.schedule_id IS NULL                 -- 임베딩 없음
         OR s.embedding_dirty = true             -- 더티 플래그
         OR s.embedding_synced_at IS NULL        -- synced_at 비어있음
         OR s.embedding_synced_at < s.updated_at -- 스케줄 수정이 더 최근(최신화 필요)
         OR e.updated_at < s.updated_at          -- 임베딩 updated_at이 더 과거(최신화 필요)
         OR s.embedding_synced_at < now() - (:staleDays || ' days')::interval  -- 주기적 갱신
      )
    ORDER BY s.updated_at DESC
    LIMIT :limit
    FOR UPDATE OF s SKIP LOCKED
)
UPDATE schedule s
   SET embedding_dirty = true,
       embedding_status = 'QUEUED',
       embedding_last_error = null
FROM candidates c
WHERE s.schedule_id = c.schedule_id
RETURNING s.schedule_id
""", nativeQuery = true)
    List<Long> markEmbeddingQueuedForBackfillOrRefreshReturning(
            @Param("limit") int limit,
            @Param("maxFailCount") int maxFailCount,
            @Param("staleDays") int staleDays
    );

    @Modifying
    @Transactional
    @Query(value = """
UPDATE schedule
   SET embedding_status = 'SYNCED',
       embedding_dirty = false,
       embedding_synced_at = now(),
       embedding_fail_count = 0,
       embedding_last_error = null
 WHERE schedule_id = :id
   AND embedding_status = 'RUNNING'
   AND embedding_dirty = false
""", nativeQuery = true)
    int markSyncedIfStillClean(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
UPDATE schedule
   SET embedding_status = 'QUEUED'
 WHERE schedule_id = :id
   AND embedding_status = 'RUNNING'
   AND embedding_dirty = true
""", nativeQuery = true)
    int requeueIfDirtyDuringRun(@Param("id") Long id);

    @Query(value = """
  SELECT s.schedule_id
  FROM schedule s
  WHERE s.embedding_status = 'QUEUED'
    AND s.updated_at < now() - interval '10 minutes'
  ORDER BY s.updated_at ASC
  LIMIT :limit
""", nativeQuery = true)
    List<Long> findStuckQueuedEmbeddingIds(@Param("limit") int limit);

    @Query(value = """
  SELECT s.schedule_id
  FROM schedule s
  WHERE s.embedding_status = 'RUNNING'
    AND s.updated_at < now() - interval '45 minutes'
  ORDER BY s.updated_at ASC
  LIMIT :limit
""", nativeQuery = true)
    List<Long> findStuckRunningEmbeddingIds(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(value = """
  UPDATE schedule
     SET embedding_status = 'QUEUED',
         embedding_dirty = true,
         embedding_last_error = 'RESCUED_FROM_RUNNING',
         updated_at = now()
   WHERE schedule_id IN (:ids)
     AND embedding_status = 'RUNNING'
""", nativeQuery = true)
    int resetRunningToQueued(@Param("ids") List<Long> ids);



}
