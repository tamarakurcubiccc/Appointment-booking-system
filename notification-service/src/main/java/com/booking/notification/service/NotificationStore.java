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

/**
 * Upisi stanja obaveštenja, izdvojeni u sopstveni bean namerno:
 *
 * <ul>
 *   <li>svaki korak mora da se komituje odvojeno - ako slanje padne, zapis sa statusom
 *       FAILED i razlogom greške mora da ostane u bazi, a to se ne bi desilo da je sve
 *       u jednoj transakciji koja se vraća nazad;</li>
 *   <li>poziv @Transactional metode iz druge metode istog bean-a ne prolazi kroz proxy,
 *       pa transakcija ni ne bi bila otvorena.</li>
 * </ul>
 */
@Service
public class NotificationStore {

    private static final Logger log = LoggerFactory.getLogger(NotificationStore.class);

    private final NotificationRepository repository;

    public NotificationStore(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Zapisuje obaveštenje koje čeka na slanje, ili pronalazi već postojeće za isti događaj.
     *
     * @return id zapisa koji treba poslati, ili prazno ako je obaveštenje već poslato
     *         (ponovna dostava iste poruke iz RabbitMQ-a)
     */
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
            // Prethodni pokušaj je pao - isti zapis se ponovo šalje, bez dupliranja.
            return Optional.of(notification.getId());
        }

        // Dve istovremene dostave istog događaja: jedinstveno ograničenje u bazi odbija drugu
        // sa 409/DataIntegrityViolation, poruka se vrati na ponovni pokušaj i tada nađe ovaj zapis.
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
