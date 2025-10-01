package com.calendarbox.backend.friendship.repository;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);
    boolean existsByRequesterIdAndAddresseeIdAndStatusIn(Long requesterId, Long addresseeId,Collection<FriendshipStatus> statuses);

    Optional<Friendship> findByIdAndAddresseeId(Long friendshipId, Long addresseeId);
    Optional<Friendship> findByIdAndAddresseeIdAndStatus(Long friendshipId, Long addresseeId,FriendshipStatus status);
    Optional<Friendship> findByIdAndRequesterId(Long friendshipId, Long requesterId);

    Page<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status, Pageable pageable);

    Page<Friendship> findByAddresseeId(Long addresseeId, Pageable pageable);

    Page<Friendship> findByRequesterId(Long requesterId, Pageable pageable);
    Page<Friendship> findByRequesterIdAndStatus(Long requesterId, FriendshipStatus status, Pageable pageable);
    Page<Friendship> findByRequesterIdAndStatusIn(Long requesterId, Collection<FriendshipStatus> statuses, Pageable pageble);

    @Query("""
    select (count(f) > 0) from Friendship f
    where (
            (f.requester.id = :a and f.addressee.id = :b)
         or (f.requester.id = :b and f.addressee.id = :a)
          )
      and f.status = com.calendarbox.backend.friendship.enums.FriendshipStatus.ACCEPTED
    """)
    boolean existsAcceptedBetween(@Param("a") Long a, @Param("b") Long b);
}
