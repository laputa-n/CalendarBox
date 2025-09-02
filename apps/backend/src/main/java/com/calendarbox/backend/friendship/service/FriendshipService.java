package com.calendarbox.backend.friendship.service;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import com.calendarbox.backend.friendship.enums.SentQueryStatus;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final MemberRepository memberRepository;

    public Long request(Long requesterId, Long addresseeId){
        if(friendshipRepository.existsByRequesterIdAndAddresseeId(requesterId,addresseeId)){
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,"이미 보낸 요청이 있습니다.");
        }

        Member requester = memberRepository.findById(requesterId).get();
        Member addressee = memberRepository.findById(addresseeId).get();

        Friendship friendship = Friendship.request(requester,addressee);

        friendshipRepository.save(friendship);
        return friendship.getId();
    }

    public void accept(Long friendshipId, Long userId){
        Friendship f = friendshipRepository.findByIdAndAddresseeId(friendshipId,userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));
        f.accept();
    }

    public void reject(Long friendshipId, Long userId){
        Friendship f = friendshipRepository.findByIdAndAddresseeId(friendshipId,userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));
        f.reject();
    }

    @Transactional(readOnly = true)
    public Page<Friendship> inbox(Long userId, Pageable pageable){
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING,pageable);
    }

    @Transactional(readOnly = true)
    public Page<Friendship> received(Long userId, @Nullable FriendshipStatus status, Pageable pageable){
        if (status == null){
            return friendshipRepository.findByAddresseeId(userId, pageable);
        }
        return friendshipRepository.findByAddresseeIdAndStatus(userId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Friendship> sent(Long requesterId, @Nullable SentQueryStatus status, Pageable pageable){
        if (status == null){
            return friendshipRepository.findByRequesterId(requesterId, pageable);
        }
        return switch(status){
            case ACCEPTED -> friendshipRepository.findByRequesterIdAndStatus(requesterId, FriendshipStatus.ACCEPTED, pageable);
            case REQUESTED -> friendshipRepository.findByRequesterIdAndStatusIn(requesterId, List.of(FriendshipStatus.PENDING,FriendshipStatus.REJECTED), pageable);
        };
    }
}
