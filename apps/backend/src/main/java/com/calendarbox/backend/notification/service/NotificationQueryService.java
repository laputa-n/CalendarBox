package com.calendarbox.backend.notification.service;

import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.dto.response.NotificationListResponse;
import com.calendarbox.backend.notification.enums.NotificationType;
import com.calendarbox.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;

    public NotificationListResponse getList(Long userId, Pageable pageable, Boolean onlyUnread, List<NotificationType> types) {
        boolean unread = Boolean.TRUE.equals(onlyUnread);
        boolean hasTypes = types != null && !types.isEmpty();

        Page<Notification> page;
        if (unread && hasTypes) {
            page = notificationRepository.findByMember_IdAndReadAtIsNullAndTypeIn(userId, types, pageable);
        } else if (unread) {
            page = notificationRepository.findByMember_IdAndReadAtIsNull(userId, pageable);
        } else if (hasTypes) {
            page = notificationRepository.findByMember_IdAndTypeIn(userId, types, pageable);
        } else {
            page = notificationRepository.findByMember_Id(userId, pageable);
        }

        return NotificationListResponse.from(page);
    }
}
