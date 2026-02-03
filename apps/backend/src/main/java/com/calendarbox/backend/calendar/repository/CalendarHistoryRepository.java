package com.calendarbox.backend.calendar.repository;

import com.calendarbox.backend.calendar.domain.CalendarHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface CalendarHistoryRepository extends JpaRepository<CalendarHistory, Long> {
    @EntityGraph(attributePaths = "actor")
    @Query(
            value = """
                select h from CalendarHistory h
                where h.calendar.id = :calendarId
                  and (:from is null or h.createdAt >= :from)
                  and (:to   is null or h.createdAt <  :to)
                order by h.createdAt desc, h.id desc
            """,
            countQuery = """
                select count(h) from CalendarHistory h
                where h.calendar.id = :calendarId
                  and (:from is null or h.createdAt >= :from)
                  and (:to   is null or h.createdAt <  :to)
            """
    )
    Page<CalendarHistory> findPage(
            @Param("calendarId") Long calendarId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
