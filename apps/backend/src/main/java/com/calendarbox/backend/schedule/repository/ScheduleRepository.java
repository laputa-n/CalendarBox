package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("""
SELECT s
FROM Schedule s
WHERE s.calendar.id IN :calendarIds
  AND s.startAt < :to
  AND s.endAt   > :from
ORDER BY s.startAt ASC, s.id ASC
""")
    List<Schedule> findAllOverlapping(@Param("calendarIds") Collection<Long> calendarIds,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    @Query("""
SELECT s
FROM Schedule s
WHERE s.calendar.id IN :calendarIds
  AND s.endAt > :from
ORDER BY s.startAt ASC, s.id ASC
""")
    List<Schedule> findAllFrom(@Param("calendarIds") Collection<Long> calendarIds,
                               @Param("from") Instant from);

    @Query("""
SELECT s
FROM Schedule s
WHERE s.calendar.id IN :calendarIds
  AND s.startAt < :to
ORDER BY s.startAt ASC, s.id ASC
""")
    List<Schedule> findAllUntil(@Param("calendarIds") Collection<Long> calendarIds,
                                @Param("to") Instant to);

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
