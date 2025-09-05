package com.calendarbox.backend.calendar.repository;

import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalendarMemberRepository extends JpaRepository<CalendarMember, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CalendarMember cm set cm.isDefault = false where cm.member.id = :memberId and cm.isDefault = true")
    int unsetDefaultForMember(@Param("memberId") Long memberId);

    boolean existsByMember_IdAndIsDefaultTrue(Long memberId);
}
