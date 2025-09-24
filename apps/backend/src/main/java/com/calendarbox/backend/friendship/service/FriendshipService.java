package com.calendarbox.backend.friendship.service;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.dto.request.FriendRequest;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import com.calendarbox.backend.friendship.enums.SentQueryStatus;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.enums.NotificationType;
import com.calendarbox.backend.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    public Long request(Long requesterId, FriendRequest request){
        Member addressee = memberRepository.findByEmail(request.query())
                .or(() -> memberRepository.findByPhoneNumber(request.query()))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if(friendshipRepository.existsByRequesterIdAndAddresseeId(requesterId,addressee.getId())){
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,"이미 보낸 요청이 있습니다.");
        }

        Member requester = memberRepository.findById(requesterId).get();

        Friendship friendship = Friendship.request(requester,addressee);



        friendshipRepository.save(friendship);
        Notification notification = Notification.builder()
                .member(addressee)
                .actor(requester)
                .type(NotificationType.RECEIVED_FRIEND_REQUEST)
                .resourceId(friendship.getId())
                .payloadJson(
                        toJson(Map.of(
                                "friendshipId", friendship.getId(),
                                "actorName", requester.getName()
                        ))
                )
                .dedupeKey("friendRequest:" + friendship.getId())
                .build();

        notificationRepository.save(notification);
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
}
