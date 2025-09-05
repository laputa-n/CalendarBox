package com.calendarbox.backend.calendar.repository;

import com.calendarbox.backend.calendar.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    boolean existsByOwner_IdAndName(Long ownerId, String name);
    boolean existsByOwner_Id(Long ownerId);
    int countByOwner_Id(Long ownerId);
}
