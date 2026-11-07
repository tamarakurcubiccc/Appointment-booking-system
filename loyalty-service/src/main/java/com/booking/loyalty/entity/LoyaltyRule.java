package com.booking.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_rules",
        indexes = @Index(name = "idx_rule_provider", columnList = "provider_id"))
public class LoyaltyRule {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID providerId;

    @Column(name = "required_appointments", nullable = false)
    private int requiredAppointments;

    @Column(name = "reward_description", nullable = false, length = 200)
    private String rewardDescription;

    @Column(name = "reward_validity_days", nullable = false)
    private int rewardValidityDays;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected LoyaltyRule() {
    }

    public LoyaltyRule(UUID providerId, int requiredAppointments, String rewardDescription, int rewardValidityDays) {
        this.providerId = providerId;
        this.requiredAppointments = requiredAppointments;
        this.rewardDescription = rewardDescription;
        this.rewardValidityDays = rewardValidityDays;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public int getRequiredAppointments() {
        return requiredAppointments;
    }

    public void setRequiredAppointments(int requiredAppointments) {
        this.requiredAppointments = requiredAppointments;
    }

    public String getRewardDescription() {
        return rewardDescription;
    }

    public void setRewardDescription(String rewardDescription) {
        this.rewardDescription = rewardDescription;
    }

    public int getRewardValidityDays() {
        return rewardValidityDays;
    }

    public void setRewardValidityDays(int rewardValidityDays) {
        this.rewardValidityDays = rewardValidityDays;
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
