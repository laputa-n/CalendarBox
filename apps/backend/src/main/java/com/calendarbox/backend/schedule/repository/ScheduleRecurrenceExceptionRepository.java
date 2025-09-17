package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleRecurrenceException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRecurrenceExceptionRepository extends JpaRepository<ScheduleRecurrenceException, Long> {
    @Query("select e from ScheduleRecurrenceException e where e.scheduleRecurrence.schedule.id = :scheduleId order by e.exceptionDate asc, e.id asc")
    List<ScheduleRecurrenceException> findByScheduleId(Long scheduleId);
    boolean existsByScheduleRecurrence_IdAndExceptionDate(Long recurrenceId, LocalDate exceptionDate);
    List<ScheduleRecurrenceException> findByScheduleRecurrence_Id(Long scheduleRecurrenceId);
}
