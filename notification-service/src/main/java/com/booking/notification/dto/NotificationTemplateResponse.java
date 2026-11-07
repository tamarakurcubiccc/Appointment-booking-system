package com.booking.notification.dto;

import com.booking.notification.entity.NotificationTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String code,
        String description,
        String subject,
        String body,
        List<String> parameters,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificationTemplateResponse from(NotificationTemplate t) {
        List<String> parameters = t.getParameters() == null || t.getParameters().isBlank()
                ? List.of()
                : List.of(t.getParameters().split(","));

        return new NotificationTemplateResponse(t.getId(), t.getCode(), t.getDescription(), t.getSubject(),
                t.getBody(), parameters, t.isActive(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
