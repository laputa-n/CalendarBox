package com.calendarbox.backend.friendship.controller;

import com.calendarbox.backend.friendship.domain.Friendship;
import com.calendarbox.backend.friendship.dto.request.FriendRequest;
import com.calendarbox.backend.friendship.dto.request.FriendRespondRequest;
import com.calendarbox.backend.friendship.dto.response.*;
import com.calendarbox.backend.friendship.enums.Action;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import com.calendarbox.backend.friendship.enums.SentQueryStatus;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.friendship.service.FriendshipService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.dto.PageResponse;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friendships")
public class FriendshipController {
    private final FriendshipService friendshipService;
    private final FriendshipRepository friendshipRepository;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> requestFriend(
            @AuthenticationPrincipal(expression= "id") Long requesterId,
            @Valid @RequestBody FriendRequest friendRequest
    ) {
        var data = friendshipService.request(requesterId, friendRequest);

        return ResponseEntity.ok(ApiResponse.ok("친구 요청을 보냈습니다.", data));
    }

    @PatchMapping("/{friendshipId}")
    public ResponseEntity<ApiResponse<FriendRespondResponse>> respond(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long friendshipId,
            @Valid @RequestBody FriendRespondRequest request
    ){
        var data = switch(request.action()){
            case ACCEPT -> friendshipService.accept(friendshipId, userId);
            case REJECT -> friendshipService.reject(friendshipId, userId);
        };

        String msg = (request.action() == Action.ACCEPT)
                ? "친구 요청을 수락했습니다."
                : "친구 요청을 거절했습니다.";

        return ResponseEntity.ok(ApiResponse.ok(msg, data));
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<PageResponse<ReceivedItemResponse>>> getReceived(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(required = false)FriendshipStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Friendship> pageResult = friendshipService.received(userId, status, pageable);

        List<ReceivedItemResponse> content = pageResult.getContent().stream()
                .map(f -> new ReceivedItemResponse(
                        f.getId(),
                        f.getRequester().getId(),
                        f.getAddressee().getId(),
                        f.getStatus(),
                        f.getCreatedAt(),
                        f.getRespondedAt()
                ))
                .toList();

        var data = new PageResponse<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("받은 친구 요청 목록입니다.",data));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PageResponse<SentItemResponse>>> getSent(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(required = false) SentQueryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page,size, Sort.by(Sort.Direction.DESC, "creadtedAt"));

        Page<Friendship> pageResult = friendshipService.sent(userId, status, pageable);

        var content = pageResult.getContent().stream()
                .map(f-> new SentItemResponse(
                        f.getId(),
                        f.getAddressee().getId(),
                        (f.getStatus() == FriendshipStatus.ACCEPTED)?"ACCEPTED":"REQUESTED",
                        f.getCreatedAt(),
                        f.getRespondedAt()

                ))
                .toList();
        var data = new PageResponse<>(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("보낸 친구 요청 목록 조회 성공", data));
    }
}
