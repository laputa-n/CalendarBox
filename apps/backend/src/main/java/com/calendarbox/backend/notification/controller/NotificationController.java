package com.calendarbox.backend.notification.controller;

import com.calendarbox.backend.notification.service.NotificationQueryService;
import com.calendarbox.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationQueryService notificationQueryService;
}
