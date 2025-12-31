package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleReminderRepository extends JpaRepository<ScheduleReminder, Long> {
    @Query("""
        select count(r) > 0
        from ScheduleReminder r
        where r.schedule.id = :scheduleId
          and r.minutesBefore = :minutesBefore
    """)
    boolean existsByScheduleIdAndMinutesBefore(@Param("scheduleId") Long scheduleId,
                                               @Param("minutesBefore") int minutesBefore);
    List<ScheduleReminder> findAllBySchedule_Id(Long scheduleId);

    boolean existsBySchedule_Id(Long scheduleId);
    Long countBySchedule_Id(Long scheduleId);


}
