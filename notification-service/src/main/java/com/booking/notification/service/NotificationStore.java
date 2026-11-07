package com.booking.notification.service;

import com.booking.notification.entity.Notification;
import com.booking.notification.entity.NotificationStatus;
import com.booking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationStore {
    private static final Logger log = LoggerFactory.getLogger(NotificationStore.class);

    private final NotificationRepository repository;

    public NotificationStore(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<UUID> prepare(UUID eventId, String templateCode, Recipient recipient,
                                 String subject, String body) {
        var existing = repository.findByEventIdAndTemplateCodeAndRecipientEmail(
                eventId, templateCode, recipient.email());

        if (existing.isPresent()) {
            var notification = existing.get();
            if (notification.getStatus() == NotificationStatus.SENT) {
                return Optional.empty();
            }

            return Optional.of(notification.getId());
        }

        var notification = new Notification(eventId, templateCode, recipient.id(), recipient.email(),
                subject, body);
        repository.save(notification);
        return Optional.of(notification.getId());
    }

    @Transactional
    public void markSent(UUID id) {
        repository.findById(id).ifPresent(Notification::markSent);
    }

    @Transactional
    public void markFailed(UUID id, String reason) {
        repository.findById(id).ifPresent(notification -> {
            notification.markFailed(reason);
            log.warn("Notification {} failed: {}", id, reason);
        });
    }
}
