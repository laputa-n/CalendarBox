package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleParticipant;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    boolean existsBySchedule_IdAndMember_IdAndStatus(Long scheduleId, Long memberId, ScheduleParticipantStatus status);
    boolean existsBySchedule_IdAndMember_Id(Long scheduleId, Long memberId);

    @Query("""
select sp
from ScheduleParticipant sp
left join fetch sp.member m
where sp.schedule.id = :scheduleId
  and (:status is null or sp.status = :status)
order by sp.invitedAt asc, sp.id asc
""")
    List<ScheduleParticipant> findAllByScheduleAndStatus(
            @Param("scheduleId") Long scheduleId,
            @Param("status") ScheduleParticipantStatus status
    );
}
