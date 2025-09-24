package com.calendarbox.backend.notification.controller;

import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.notification.dto.response.NotificationListResponse;
import com.calendarbox.backend.notification.enums.NotificationType;
import com.calendarbox.backend.notification.service.NotificationQueryService;
import com.calendarbox.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean onlyUnread,
            @RequestParam(required = false)List<NotificationType> types
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var data = notificationQueryService.getList(userId,pageable,onlyUnread,types);

        return ResponseEntity.ok(ApiResponse.ok("알림 목록 조회 성공", data));
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> readNotification(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long notificationId
    ){
        notificationService.read(userId,notificationId);

        return ResponseEntity.ok(ApiResponse.ok("알림 읽음 처리 성공", null));
    }
}
