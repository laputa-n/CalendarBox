package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleRecurrenceRepository extends JpaRepository<ScheduleRecurrence, Long> {
    Optional<ScheduleRecurrence> findBySchedule_Id(Long scheduleId);
    boolean existsBySchedule_Id(Long scheduleId);
    List<ScheduleRecurrence> findAllBySchedule_Id(Long scheduleId);
    Long countBySchedule_Id(Long scheduleId);

}
