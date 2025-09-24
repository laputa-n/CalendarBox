package com.calendarbox.backend.notification.dto.response;

import com.calendarbox.backend.notification.domain.Notification;
import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationListResponse(
        List<NotificationListItem> notifications,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static NotificationListResponse from(Page<Notification> page) {
        var items = page.getContent().stream().map(NotificationListItem::from).toList();
        return new NotificationListResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
