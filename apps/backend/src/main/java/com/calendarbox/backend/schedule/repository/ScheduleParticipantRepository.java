package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleParticipant;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    boolean existsBySchedule_IdAndMember_IdAndStatus(Long scheduleId, Long memberId, ScheduleParticipantStatus status);
}
