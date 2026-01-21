package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.calendar.dto.response.InvitedCalendarMemberItem;
import com.calendarbox.backend.schedule.domain.ScheduleParticipant;
import com.calendarbox.backend.schedule.dto.response.InvitedScheduleParticipantItem;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    Optional<ScheduleParticipant> findByIdAndSchedule_Id(Long participantId, Long scheduleId);
    boolean existsBySchedule_IdAndMember_IdAndStatus(Long scheduleId, Long memberId, ScheduleParticipantStatus status);
    boolean existsBySchedule_IdAndMember_IdAndStatusIn(Long scheduleId, Long memberId, List<ScheduleParticipantStatus> statuses);
    boolean existsBySchedule_IdAndMember_Id(Long scheduleId, Long memberId);
    boolean existsBySchedule_Id(Long scheduleId);
    @Query(
            value = """
            select sp
            from ScheduleParticipant sp
            left join fetch sp.member m
            where sp.schedule.id = :scheduleId
              and (:status is null or sp.status = :status)
            """,
            countQuery = """
                    select count(sp)
                    from ScheduleParticipant sp
                    where sp.schedule.id = :scheduleId
                        and (:status is null or sp.status = :status)
            """
    )
    Page<ScheduleParticipant> findAllByScheduleAndStatus(
            @Param("scheduleId") Long scheduleId,
            @Param("status") ScheduleParticipantStatus status,
            Pageable pageable
    );

    Long countBySchedule_Id(Long scheduleId);

    @Query(value = """
        select new com.calendarbox.backend.schedule.dto.response.InvitedScheduleParticipantItem(
            sp.id,s.id,s.title,s.startAt,s.endAt, i.id, i.name, sp.invitedAt
        ) from ScheduleParticipant sp
        join sp.schedule s
        join sp.inviter i
        where sp.member.id = :memberId
            and sp.status = com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus.INVITED
        """, countQuery = """
        select count(sp)
        from ScheduleParticipant sp
        where sp.member.id = :memberId
            and sp.status = com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus.INVITED
    """)
    Page<InvitedScheduleParticipantItem> findInvitedScheduleParticipantList(@Param("memberId") Long memberId, Pageable pageable);
}
