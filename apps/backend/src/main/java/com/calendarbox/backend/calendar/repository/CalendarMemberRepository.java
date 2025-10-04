package com.calendarbox.backend.calendar.repository;

import com.calendarbox.backend.calendar.domain.CalendarMember;
import com.calendarbox.backend.calendar.dto.response.CalendarListItem;
import com.calendarbox.backend.calendar.dto.response.CalendarMemberItem;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CalendarMemberRepository extends JpaRepository<CalendarMember, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CalendarMember cm set cm.isDefault = false where cm.member.id = :memberId and cm.isDefault = true")
    int unsetDefaultForMember(@Param("memberId") Long memberId);

    @Query("select count(*) from CalendarMember cm where cm.calendar.id = :calendarId and cm.status = com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED")
    int countCalendarMembersByCalendarId(@Param("calendarId") Long calendarId);

    boolean existsByMember_IdAndIsDefaultTrue(Long memberId);
    boolean existsByMember_IdAndCalendar_Id(Long memberId, Long calendarId);

    @Query("""
    select new com.calendarbox.backend.calendar.dto.response.CalendarListItem(
        c.id, c.name, c.type, cm.status, c.visibility, cm.isDefault
    )
    from CalendarMember cm
    join cm.calendar c
    where cm.member.id = :targetId
      and (:status is null or cm.status = :status)
      and (:type is null or c.type = :type)
      and (:visibility is null or c.visibility = :visibility)
    order by c.name asc, c.id desc
    """)
    Page<CalendarListItem> findSelf(
            @Param("targetId") Long targetId,
            @Param("status") CalendarMemberStatus status,
            @Param("type") CalendarType type,
            @Param("visibility") Visibility visibility,
            Pageable pageable
    );

    @Query("""
select new com.calendarbox.backend.calendar.dto.response.CalendarListItem(
  c.id, c.name, c.type, null, null, null
)
from CalendarMember cmTarget
join cmTarget.calendar c
where cmTarget.member.id = :targetId
  and cmTarget.status = com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED
  and (
        c.visibility = com.calendarbox.backend.calendar.enums.Visibility.PUBLIC
        or c.visibility = com.calendarbox.backend.calendar.enums.Visibility.PROTECTED
      )
  and (:type is null or c.type = :type)
order by c.name asc, c.id desc
""")
    Page<CalendarListItem> findFriendVisible(
            @Param("targetId") Long targetId,
            @Param("type") CalendarType type,
            Pageable pageable
    );

    boolean existsByCalendar_IdAndMember_IdAndIsDefaultTrue(Long calendarId,Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CalendarMember cm where cm.calendar.id = :calendarId")
    int deleteByCalendarId(@Param("calendarId") Long calendarId);

    @Query("""
        select cm
        from CalendarMember cm
        join cm.calendar c
        where cm.member.id = :memberId
          and cm.status = com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED
        order by
          case when c.type = com.calendarbox.backend.calendar.enums.CalendarType.PERSONAL then 0 else 1 end,
          c.createdAt desc
        """)
    List<CalendarMember> findDefaultCandidate(@Param("memberId") Long memberId, Pageable pageable);

    @Query("""
    select cm
    from CalendarMember cm
    where cm.calendar.id = :calendarId
      and cm.member.id in (:memberIds)
    """)
    List<CalendarMember> findByCalendarIdAndMemberIds(
            @Param("calendarId") Long calendarId,
            @Param("memberIds") Collection<Long> memberIds
    );


    boolean existsByCalendar_IdAndMember_IdAndStatus(Long calendarId, Long memberId, CalendarMemberStatus status);

    @Query("""
        select new com.calendarbox.backend.calendar.dto.response.CalendarMemberItem(
        cm.id, m.id, m.name, cm.status, cm.createdAt, cm.respondedAt
        )
        from CalendarMember cm
        join cm.member m
        where cm.calendar.id = :calendarId
        and (:status is null or cm.status = :status)
        order by m.name asc, cm.id asc
    """)
    Page<CalendarMemberItem> findMembersOrderByNameAsc(@Param("calendarId") Long calendarId, @Param("status") CalendarMemberStatus status, Pageable pageable);

    @Query("""
        select new com.calendarbox.backend.calendar.dto.response.CalendarMemberItem(
        cm.id, m.id, m.name, cm.status, cm.createdAt, cm.respondedAt
        )
        from CalendarMember cm
        join cm.member m
        where cm.calendar.id = :calendarId
        and (:status is null or cm.status = :status)
        order by m.name desc, cm.id asc
    """)
    Page<CalendarMemberItem> findMembersOrderByNameDesc(@Param("calendarId") Long calendarId, @Param("status") CalendarMemberStatus status,Pageable pageable);

    @Query("""
        select new com.calendarbox.backend.calendar.dto.response.CalendarMemberItem(
        cm.id, m.id, m.name, cm.status, cm.createdAt, cm.respondedAt
        )
        from CalendarMember cm
        join cm.member m
        where cm.calendar.id = :calendarId
        and (:status is null or cm.status = :status)
        order by cm.createdAt asc, cm.id asc
    """)
    Page<CalendarMemberItem> findMembersOrderByCreatedAtAsc(@Param("calendarId") Long calendarId, @Param("status") CalendarMemberStatus status,Pageable pageable);

    @Query("""
        select new com.calendarbox.backend.calendar.dto.response.CalendarMemberItem(
        cm.id, m.id, m.name, cm.status, cm.createdAt, cm.respondedAt
        )
        from CalendarMember cm
        join cm.member m
        where cm.calendar.id = :calendarId
        and (:status is null or cm.status = :status)
        order by cm.createdAt desc, cm.id asc
    """)
    Page<CalendarMemberItem> findMembersOrderByCreatedAtDesc(@Param("calendarId") Long calendarId, @Param("status") CalendarMemberStatus status,Pageable pageable);

    @Query("""
    select cm.calendar.id
    from CalendarMember cm
    where cm.member.id = :memberId
      and cm.status in :statuses
    """)
    List<Long> findCalendarIdsByMemberIdAndStatuses(@Param("memberId")Long memberId, @Param("statuses")List<CalendarMemberStatus> statuses);

    @Query("""
select case when count(cm) > 0 then true else false end
from CalendarMember cm
join Friendship f
where cm.calendar.id = :calendarId
  and cm.status = com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED
  and f.status = com.calendarbox.backend.friendship.enums.FriendshipStatus.ACCEPTED
  and (
       (f.requester.id = :viewerId and f.addressee.id = cm.member.id)
    or (f.addressee.id = :viewerId and f.requester.id = cm.member.id)
  )
""")
    boolean existsFriendshipWithAnyAcceptedMember(@Param("calendarId") Long calendarId,
                                                  @Param("viewerId") Long viewerId);

    /** 그룹 멤버(PENDING 포함) 체크에 쓰는 exists */
    boolean existsByCalendar_IdAndMember_IdAndStatusIn(Long calendarId, Long memberId,
                                                       Collection<com.calendarbox.backend.calendar.enums.CalendarMemberStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
update CalendarMember cm
   set cm.isDefault = true
 where cm.member.id   = :memberId
   and cm.calendar.id = :calendarId
   and cm.status = com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED
""")
    int setDefault(@Param("memberId") Long memberId,
                   @Param("calendarId") Long calendarId);
}
