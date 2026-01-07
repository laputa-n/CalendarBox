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

    @Query(value = """
        SELECT s.schedule_id
        FROM schedule s
        LEFT JOIN schedule_embedding e ON e.schedule_id = s.schedule_id
        WHERE e.schedule_id IS NULL
        ORDER BY s.updated_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findScheduleIdsWithoutEmbedding(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE schedule
           SET embedding_status = 'RUNNING', updated_at = now()
         WHERE schedule_id = :id
           AND embedding_dirty = true
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
    int markEmbeddingQueued(@Param("id") Long id);


    @Modifying
    @Transactional
    @Query(value = """
UPDATE schedule s
   SET embedding_dirty = true,
       embedding_status = 'QUEUED',
       embedding_last_error = null
 WHERE s.schedule_id IN (:ids)
   AND NOT EXISTS (SELECT 1 FROM schedule_embedding e WHERE e.schedule_id = s.schedule_id)
RETURNING s.schedule_id
""", nativeQuery = true)
    List<Long> markEmbeddingQueuedForBackfillReturning(@Param("ids") List<Long> ids);

}
