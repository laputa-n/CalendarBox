package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
}
