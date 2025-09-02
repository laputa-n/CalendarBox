package com.calendarbox.backend.friendship.controller;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.dto.request.FriendRequest;
import com.calendarbox.backend.friendship.dto.response.FriendRequestResponse;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.friendship.service.FriendshipService;
import com.calendarbox.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friendships")
public class FriendshipController {
    private final FriendshipService friendshipService;
    private final FriendshipRepository friendshipRepository;

    //친구 요청
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> requestFriend(
            @AuthenticationPrincipal(expression= "id") Long requesterId,
            @Valid @RequestBody FriendRequest friendRequest
    ) {
        Long addresseeId = friendRequest.addresseeId();

        Long friendshipId = friendshipService.request(requesterId, addresseeId);

        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalStateException("Friendship not found after create"));

        FriendRequestResponse friendRequestResponse = new FriendRequestResponse(friendshipId,requesterId,addresseeId,f.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.ok("친구 요청을 보냈습니다.", friendRequestResponse));
    }

}
