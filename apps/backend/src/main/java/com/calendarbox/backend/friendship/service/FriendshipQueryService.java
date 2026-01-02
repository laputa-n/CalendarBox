package com.calendarbox.backend.friendship.service;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.dto.response.FriendListItem;
import com.calendarbox.backend.friendship.dto.response.ReceivedItemResponse;
import com.calendarbox.backend.friendship.dto.response.SentItemResponse;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import com.calendarbox.backend.friendship.enums.SentQueryStatus;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendshipQueryService {
    private final FriendshipRepository friendshipRepository;
    private final MemberRepository memberRepository;

    public PageResponse<ReceivedItemResponse> received(Long userId, @Nullable FriendshipStatus status, Pageable pageable){

        Page<Friendship> pageResult;
        if (status == null){
            pageResult = friendshipRepository.findByAddresseeId(userId, pageable);
        } else{
            pageResult = friendshipRepository.findByAddresseeIdAndStatus(userId, status, pageable);
        }

        List<ReceivedItemResponse> content = pageResult.getContent().stream()
                .map(f -> new ReceivedItemResponse(
                        f.getId(),
                        f.getRequester().getId(),
                        f.getAddressee().getId(),
                        f.getRequester().getName(),
                        f.getStatus(),
                        f.getCreatedAt(),
                        f.getRespondedAt()
                ))
                .toList();

        return new PageResponse<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast(),
                pageResult.hasNext(),
                pageResult.hasPrevious()
        );
    }

    @Transactional
    public PageResponse<SentItemResponse> sent(Long requesterId, @Nullable SentQueryStatus status, Pageable pageable){
        Page<Friendship> pageResult;
        if (status == null){
            pageResult =  friendshipRepository.findByRequesterId(requesterId, pageable);
        } else {
            pageResult =  switch(status){
                case ACCEPTED -> friendshipRepository.findByRequesterIdAndStatus(requesterId, FriendshipStatus.ACCEPTED, pageable);
                case REQUESTED -> friendshipRepository.findByRequesterIdAndStatusIn(requesterId, List.of(FriendshipStatus.PENDING,FriendshipStatus.REJECTED), pageable);
            };
        }

        var content = pageResult.getContent().stream()
                .map(f-> new SentItemResponse(
                        f.getId(),
                        f.getAddressee().getId(),
                        f.getAddressee().getName(),
                        (f.getStatus() == FriendshipStatus.ACCEPTED)?"ACCEPTED":"REQUESTED",
                        f.getCreatedAt(),
                        f.getRespondedAt()

                ))
                .toList();

        return new PageResponse<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast(),
                pageResult.hasNext(),
                pageResult.hasPrevious()
        );
    }

    public PageResponse<FriendListItem> getList(Long userId,Pageable pageable){
        Page<FriendListItem> page = friendshipRepository.findAcceptedFriendList(userId, pageable);

        return PageResponse.of(page);
    }
}
