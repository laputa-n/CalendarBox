package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.SchedulePlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulePlaceRepository extends JpaRepository<SchedulePlace, Long> {
}
