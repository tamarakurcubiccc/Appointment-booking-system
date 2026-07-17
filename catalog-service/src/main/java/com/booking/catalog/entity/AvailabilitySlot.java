package com.booking.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Konkretan termin za uslugu koji može da se rezerviše.
 * Jedinstveno ograničenje (service_id, start_time) sprečava duplirane termine,
 * dok @Version kolona zajedno sa pesimističkim zaključavanjem pri rezervaciji
 * sprečava da dva klijenta zauzmu isti termin.
 */
@Entity
@Table(
        name = "availability_slots",
        uniqueConstraints = @UniqueConstraint(name = "uk_slot_service_start", columnNames = {"service_id", "start_time"}),
        indexes = {
                @Index(name = "idx_slot_service_start", columnList = "service_id,start_time"),
                @Index(name = "idx_slot_status", columnList = "status")
        }
)
public class AvailabilitySlot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Version
    private long version;

    protected AvailabilitySlot() {
    }

    public AvailabilitySlot(ServiceOffering service, Instant startTime, Instant endTime) {
        this.service = service;
        this.providerId = service.getProviderId();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getId() {
        return id;
    }

    public ServiceOffering getService() {
        return service;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public boolean isBookable() {
        return status == SlotStatus.AVAILABLE && startTime.isAfter(Instant.now());
    }
}
