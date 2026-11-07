package com.booking.notification.dto;

import com.booking.notification.entity.Notification;
import com.booking.notification.entity.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String templateCode,
        UUID recipientId,
        String recipientEmail,
        String subject,
        String body,
        NotificationStatus status,
        String failureReason,
        int attempts,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTemplateCode(), n.getRecipientId(), n.getRecipientEmail(),
                n.getSubject(), n.getBody(), n.getStatus(), n.getFailureReason(), n.getAttempts(),
                n.getCreatedAt(), n.getSentAt());
    }
}
