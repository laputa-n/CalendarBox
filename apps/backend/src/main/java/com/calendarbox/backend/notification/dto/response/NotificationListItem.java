package com.calendarbox.backend.notification.dto.response;

import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.notification.domain.Notification;
import com.calendarbox.backend.notification.enums.NotificationType;

import java.time.Instant;

public record NotificationListItem(
        Long id,
        NotificationType type,
        String payloadJson,
        Instant createdAt,
        Instant readAt,
        ActorSummary actor
) {
    public static NotificationListItem from(Notification n) {
        return new NotificationListItem(
                n.getId(),
                n.getType(),
                n.getPayloadJson(),
                n.getCreatedAt(),
                n.getReadAt(),
                n.getActor() != null ? ActorSummary.from(n.getActor()) : null
        );
    }

    public record ActorSummary(Long id, String name) {
        public static ActorSummary from(Member m) {
            return new ActorSummary(m.getId(), m.getName());
        }
    }
}
