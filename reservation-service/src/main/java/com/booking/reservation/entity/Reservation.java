package com.booking.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Rezervacija. Id-jevi klijenta, pružaoca usluge, usluge i termina su strani ključevi
 * ka *drugim servisima* - namerno nisu JPA relacije: svaki servis poseduje sopstvenu bazu.
 * Dvostruka rezervacija se sprečava u catalog-service (SELECT ... FOR UPDATE nad terminom);
 * provera sa ove strane je druga, jeftina linija odbrane.
 */
@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "idx_reservation_client", columnList = "client_id"),
                @Index(name = "idx_reservation_provider", columnList = "provider_id"),
                @Index(name = "idx_reservation_slot_status", columnList = "slot_id,status"),
                @Index(name = "idx_reservation_start", columnList = "start_time")
        }
)
public class Reservation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "service_name", length = 140)
    private String serviceName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Reservation() {
    }

    public Reservation(UUID clientId, UUID providerId, UUID serviceId, UUID slotId,
                       String serviceName, Instant startTime, Instant endTime, String note) {
        this.clientId = clientId;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.slotId = slotId;
        this.serviceName = serviceName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.note = note;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
