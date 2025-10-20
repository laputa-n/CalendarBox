package com.calendarbox.backend.calendar.repository;

import com.calendarbox.backend.calendar.domain.CalendarHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarHistoryRepository extends JpaRepository<CalendarHistory, Long> {
}
