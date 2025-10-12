package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
    select distinct s
    from Schedule s
        left join s.participants p
        left join s.todos t
        left join s.places sp
    where s.calendar.id in :calendarIds
        and (
            lower(s.title) like lower(concat('%',:q,'%'))
          or lower(s.memo) like lower(concat('%',:q,'%'))
          or lower(p.name) like lower(concat('%',:q,'%'))
          or lower(t.content) like lower(concat('%',:q,'%'))
          or lower (sp.name) like lower(concat('%',:q,'%'))
        )
    order by s.startAt ASC, s.id ASC
    """)
    List<Schedule> searchByKeyword(@Param("calendarIds") Collection<Long> calendarIds,@Param("q") String q);
}
