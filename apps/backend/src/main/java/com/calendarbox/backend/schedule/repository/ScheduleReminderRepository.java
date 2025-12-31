package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleReminderRepository extends JpaRepository<ScheduleReminder, Long> {
    boolean existsBySchedule_IdAndMinutesBefore(Long scheduleId, int minutesBefore);
    List<ScheduleReminder> findAllBySchedule_Id(Long scheduleId);

    boolean existsBySchedule_Id(Long scheduleId);
    Long countBySchedule_Id(Long scheduleId);


}
