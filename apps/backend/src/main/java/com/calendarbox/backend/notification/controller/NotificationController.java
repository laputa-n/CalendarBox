package com.calendarbox.backend.notification.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.notification.dto.response.NotificationListResponse;
import com.calendarbox.backend.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림")
@Controller
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationQueryService notificationQueryService;

    @Operation(
            summary = "알림 목록 조회",
            description = "알림 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var data = notificationQueryService.getList(userId,pageable);

        return ResponseEntity.ok(ApiResponse.ok("알림 목록 조회 성공", data));
    }
}
