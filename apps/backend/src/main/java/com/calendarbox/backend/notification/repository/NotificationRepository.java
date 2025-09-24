package com.calendarbox.backend.notification.repository;

import com.calendarbox.backend.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
