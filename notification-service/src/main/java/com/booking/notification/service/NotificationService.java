package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import com.booking.notification.dto.NotificationResponse;
import com.booking.notification.mail.EmailSender;
import com.booking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationTemplateService templates;
    private final TemplateRenderer renderer;
    private final NotificationStore store;
    private final EmailSender emailSender;

    public NotificationService(NotificationRepository repository,
                               NotificationTemplateService templates,
                               TemplateRenderer renderer,
                               NotificationStore store,
                               EmailSender emailSender) {
        this.repository = repository;
        this.templates = templates;
        this.renderer = renderer;
        this.store = store;
        this.emailSender = emailSender;
    }

    public void dispatch(UUID eventId, String templateCode, Recipient recipient, Map<String, String> parameters) {
        var template = templates.findActive(templateCode);
        if (template.isEmpty()) {
            log.warn("No active template {} - notification for event {} skipped", templateCode, eventId);
            return;
        }
        if (recipient.email() == null || recipient.email().isBlank()) {
            log.warn("Recipient {} has no e-mail address - template {} skipped", recipient.id(), templateCode);
            return;
        }

        String subject = renderer.render(template.get().getSubject(), parameters);
        String body = renderer.render(template.get().getBody(), parameters);

        var id = store.prepare(eventId, templateCode, recipient, subject, body);
        if (id.isEmpty()) {
            log.debug("Notification {} for event {} already sent - duplicate delivery ignored", templateCode, eventId);
            return;
        }

        try {
            emailSender.send(recipient.email(), subject, body);
            store.markSent(id.get());
        } catch (RuntimeException ex) {
            store.markFailed(id.get(), ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMine(UUID recipientId, Pageable pageable) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(UUID id, UUID callerId, boolean admin) {
        var notification = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Obaveštenje " + id + " nije pronađeno"));

        if (!admin && !callerId.equals(notification.getRecipientId())) {
            throw ApiException.forbidden("Pristup nije dozvoljen");
        }
        return NotificationResponse.from(notification);
    }
}
