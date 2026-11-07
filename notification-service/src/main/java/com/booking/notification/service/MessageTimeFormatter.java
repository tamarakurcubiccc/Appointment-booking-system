package com.booking.notification.service;

import com.booking.notification.config.NotificationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class MessageTimeFormatter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm");

    private final ZoneId zone;

    public MessageTimeFormatter(NotificationProperties properties) {
        this.zone = ZoneId.of(properties.timeZone());
    }

    public String format(Instant instant) {
        return instant == null ? "" : FORMATTER.format(instant.atZone(zone));
    }
}
