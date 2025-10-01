package com.calendarbox.backend.friendship.service;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.dto.request.FriendRequest;
import com.calendarbox.backend.friendship.dto.response.FriendRequestResponse;
import com.calendarbox.backend.friendship.dto.response.FriendRespondResponse;
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

import java.time.Instant;
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

    public FriendRequestResponse request(Long requesterId, FriendRequest request){
        Member requester = memberRepository.findById(requesterId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member addressee = memberRepository.findByEmail(request.query())
                .or(() -> memberRepository.findByPhoneNumber(request.query()))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (requester.getId().equals(addressee.getId())) {
            throw new BusinessException(ErrorCode.FRIENDSHIP_SELF_REQUEST);
        }
        if(friendshipRepository.existsByRequesterIdAndAddresseeIdAndStatusIn(requesterId,addressee.getId(),List.of(FriendshipStatus.ACCEPTED, FriendshipStatus.PENDING))
        || friendshipRepository.existsByRequesterIdAndAddresseeIdAndStatusIn(addressee.getId(),requester.getId(),List.of(FriendshipStatus.ACCEPTED, FriendshipStatus.PENDING))){
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,"이미 보낸 요청이 있습니다.");
        }

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

        return new FriendRequestResponse(friendship.getId(),requester.getId(), addressee.getId(), friendship.getCreatedAt());
    }

    public FriendRespondResponse accept(Long friendshipId, Long userId){
        Friendship f = friendshipRepository.findByIdAndAddresseeIdAndStatus(friendshipId,userId,FriendshipStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_INVALID_STATE));
        f.accept();

        return new FriendRespondResponse(
                f.getId(), f.getRequester().getId(), f.getAddressee().getId(),
                f.getStatus(), f.getCreatedAt(), f.getRespondedAt()
        );
    }

    public FriendRespondResponse reject(Long friendshipId, Long userId){
        Friendship f = friendshipRepository.findByIdAndAddresseeIdAndStatus(friendshipId,userId,FriendshipStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_INVALID_STATE));
        f.reject();

        return new FriendRespondResponse(
                f.getId(), f.getRequester().getId(), f.getAddressee().getId(),
                f.getStatus(), f.getCreatedAt(), f.getRespondedAt()
        );
    }

    public void delete(Long userId,Long friendshipId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Friendship f = friendshipRepository.findById(friendshipId).orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));
        if(!f.getRequester().getId().equals(user.getId()) && !f.getAddressee().getId().equals(user.getId())){
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        if(f.getStatus() != FriendshipStatus.ACCEPTED) throw new BusinessException(ErrorCode.FRIENDSHIP_INVALID_STATE);

        friendshipRepository.delete(f);
    }


    @Transactional(readOnly = true)
    public Page<Friendship> inbox(Long userId, Pageable pageable){
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING,pageable);
    }


    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "알림 페이로드 직렬화 실패");
        }
    }
}
