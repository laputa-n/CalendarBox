package com.calendarbox.backend.friendship.controller;

import com.calendarbox.backend.friendship.dto.request.FriendRequest;
import com.calendarbox.backend.friendship.dto.request.FriendRespondRequest;
import com.calendarbox.backend.friendship.dto.response.*;
import com.calendarbox.backend.friendship.enums.Action;
import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import com.calendarbox.backend.friendship.enums.SentQueryStatus;
import com.calendarbox.backend.friendship.repository.FriendshipRepository;
import com.calendarbox.backend.friendship.service.FriendshipQueryService;
import com.calendarbox.backend.friendship.service.FriendshipService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Friendship", description = "친구")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friendships")
public class FriendshipController {
    private final FriendshipService friendshipService;
    private final FriendshipQueryService friendshipQueryService;

    @Operation(
            summary = "친구 요청",
            description = "친구 요청을 보냅니다."
    )
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> requestFriend(
            @AuthenticationPrincipal(expression= "id") Long requesterId,
            @Valid @RequestBody FriendRequest friendRequest
    ) {
        var data = friendshipService.request(requesterId, friendRequest);

        return ResponseEntity.ok(ApiResponse.ok("친구 요청을 보냈습니다.", data));
    }

    @Operation(
            summary = "친구 요청 응답",
            description = "친구 요청에 응답합니다."
    )
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

    @Operation(
            summary = "받은 친구 요청 조회",
            description = "받은 친구 요청 목록을 조회합니다."
    )
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<PageResponse<ReceivedItemResponse>>> getReceived(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(required = false)FriendshipStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        var data = friendshipQueryService.received(userId, status, pageable);

        return ResponseEntity.ok(ApiResponse.ok("받은 친구 요청 목록 조회 성공",data));
    }

    @Operation(
            summary = "보낸 친구 요청 조회",
            description = "보낸 친구 요청 목록을 조회합니다."
    )
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PageResponse<SentItemResponse>>> getSent(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam(required = false) SentQueryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page,size, Sort.by(Sort.Direction.DESC, "createdAt"));

        var data = friendshipQueryService.sent(userId,status,pageable);

        return ResponseEntity.ok(ApiResponse.ok("보낸 친구 요청 목록 조회 성공", data));
    }

    @Operation(
            summary = "친구 삭제",
            description = "해당 친구를 삭제합니다."
    )
    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal(expression="id")Long userId,
            @PathVariable Long friendshipId
    ){
        friendshipService.delete(userId, friendshipId);
        return ResponseEntity.ok(ApiResponse.ok("친구 삭제 완료",null));
    }

//    @Operation(
//            summary = "친구 목록 조회",
//            description = "친구 목록을 조회합니다."
//    )
//    @GetMapping
//    public
}
