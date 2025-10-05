package com.calendarbox.backend.member.repository;

import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.dto.response.MemberSearchItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    @Query("""
select new com.calendarbox.backend.member.dto.response.MemberSearchItem(
  m.id, m.name
)
from Member m
where m.deletedAt is null
  and m.id <> :viewerId
  and (
       (:emailToken is not null and lower(m.email) like concat(:emailToken, '%'))
    or (:phoneToken is not null and concat('', function('regexp_replace', coalesce(m.phoneNumber, ''), '[^0-9]', '', 'g')) like concat(:phoneToken, '%'))
  )
""")
    Page<MemberSearchItem> searchByEmailOrPhone(
            @Param("viewerId") Long viewerId,
            @Param("emailToken") String emailToken,
            @Param("phoneToken") String phoneToken,
            Pageable pageable);
}
