package com.calendarbox.backend.notification.service;


import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    public void read(Long userId,Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if(!notification.getMember().getId().equals(userId)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        notification.read();
    }
}
