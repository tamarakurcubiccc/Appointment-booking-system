package com.booking.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Jedno poslato (ili neuspelo) obaveštenje. Čuva se renderovan tekst, a ne referenca na
 * šablon: kasnija izmena šablona ne sme da promeni ono što je korisnik stvarno dobio.
 *
 * <p>Jedinstveno ograničenje nad (event_id, template_code, recipient_email) čini potrošača
 * idempotentnim - ponovna dostava iste poruke iz RabbitMQ-a, nakon prolazne greške,
 * ne pravi duplirano obaveštenje.
 */
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_event_template_recipient",
                columnNames = {"event_id", "template_code", "recipient_email"}),
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
                @Index(name = "idx_notification_created", columnList = "created_at")
        }
)
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    /** Id domenskog događaja koji je izazvao obaveštenje. */
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "template_code", nullable = false, length = 60)
    private String templateCode;

    /** Id primaoca iz user-service; null ako primalac nije korisnik sistema. */
    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "recipient_email", nullable = false, length = 180)
    private String recipientEmail;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(UUID eventId, String templateCode, UUID recipientId, String recipientEmail,
                        String subject, String body) {
        this.eventId = eventId;
        this.templateCode = templateCode;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.failureReason = null;
        this.attempts++;
    }

    public void markFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 500));
        this.attempts++;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
