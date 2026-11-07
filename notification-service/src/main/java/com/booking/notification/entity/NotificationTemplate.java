package com.booking.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Šablon obaveštenja. Tekst sadrži parametre u obliku dvostrukih vitičastih zagrada
 * koji se popunjavaju u trenutku slanja, pa administrator može da menja sadržaj
 * obaveštenja bez izmene koda.
 */
@Entity
@Table(name = "notification_templates",
        indexes = @Index(name = "idx_template_code", columnList = "code", unique = true))
public class NotificationTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    /** Stabilna šifra po kojoj servis pronalazi šablon, npr. RESERVATION_CONFIRMED_CLIENT. */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(length = 200)
    private String description;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    /**
     * Imena parametara pronađena u tekstu, razdvojena zapetom. Izvedeno polje - popunjava
     * ga servis pri svakom čuvanju, da administrator vidi koje parametre šablon očekuje.
     */
    @Column(length = 500)
    private String parameters;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected NotificationTemplate() {
    }

    public NotificationTemplate(String code, String description, String subject, String body) {
        this.code = code;
        this.description = description;
        this.subject = subject;
        this.body = body;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }
}
