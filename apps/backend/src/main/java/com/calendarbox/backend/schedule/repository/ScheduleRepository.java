package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}
