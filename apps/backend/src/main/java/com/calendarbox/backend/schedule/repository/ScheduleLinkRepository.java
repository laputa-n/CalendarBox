package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleLinkRepository extends JpaRepository<ScheduleLink, Long> {
}
