package com.calendarbox.backend.friendship.repository;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    Optional<Friendship> findByIdAndAddresseeId(Long friendshipId, Long addresseeId);
    Optional<Friendship> findByIdAndRequesterId(Long friendshipId, Long requesterId);

    Page<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status, Pageable pageable);
}
