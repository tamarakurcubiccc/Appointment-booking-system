package com.booking.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifications")
public record NotificationProperties(
        boolean mailEnabled,
        String fromAddress,
        String timeZone,
        String activationBaseUrl,
        String passwordResetBaseUrl
) {
}
