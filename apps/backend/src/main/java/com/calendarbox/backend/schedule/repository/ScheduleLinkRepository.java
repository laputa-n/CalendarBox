package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleLinkRepository extends JpaRepository<ScheduleLink, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO schedule_link (schedule_id, url, label)
        SELECT :dstId, url, label
        FROM schedule_link
        WHERE schedule_id = :srcId
        """, nativeQuery = true)
    void copyAll(@Param("srcId") Long srcId, @Param("dstId") Long dstId);

    boolean existsBySchedule_Id(Long scheduleId);

    Long countBySchedule_Id(Long scheduleId);


}
