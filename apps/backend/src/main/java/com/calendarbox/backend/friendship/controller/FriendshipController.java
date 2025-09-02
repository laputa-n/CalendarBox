package com.calendarbox.backend.friendship.controller;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.dto.request.FriendRequest;
import com.calendarbox.backend.friendship.dto.request.FriendRespondRequest;
import com.calendarbox.backend.friendship.dto.response.FriendRequestResponse;
import com.calendarbox.backend.friendship.dto.response.FriendRespondResponse;
import com.calendarbox.backend.friendship.enums.Action;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.friendship.service.FriendshipService;
import com.calendarbox.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{friendshipId}")
    public ResponseEntity<ApiResponse<FriendRespondResponse>> respond(
            @PathVariable Long friendshipId,
            @AuthenticationPrincipal(expression = "id") Long userId,
            @Valid @RequestBody FriendRespondRequest request
    ){
        switch(request.action()){
            case ACCEPT -> friendshipService.accept(friendshipId, userId);
            case REJECT -> friendshipService.reject(friendshipId, userId);
        }

        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalStateException("Friendship not found after respond"));

        var data = new FriendRespondResponse(
                f.getId(),
                f.getRequester().getId(),
                f.getAddressee().getId(),
                f.getStatus(),
                f.getCreatedAt(),
                f.getRespondedAt()
        );

        String msg = (request.action() == Action.ACCEPT)
                ? "친구 요청을 수락했습니다."
                : "친구 요청을 거절했습니다.";

        return ResponseEntity.ok(ApiResponse.ok(msg, data));
    }

}
