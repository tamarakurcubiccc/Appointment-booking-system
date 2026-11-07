package com.booking.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "loyalty_rewards",
        indexes = {
                @Index(name = "idx_reward_client", columnList = "client_id"),
                @Index(name = "idx_reward_client_provider_status", columnList = "client_id,provider_id,status"),
                @Index(name = "idx_reward_reservation", columnList = "reservation_id")
        }
)
public class LoyaltyReward {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID clientId;

    @Column(name = "provider_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID providerId;

    @Column(name = "rule_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID ruleId;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardStatus status = RewardStatus.AVAILABLE;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "reservation_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID reservationId;

    protected LoyaltyReward() {
    }

    public LoyaltyReward(UUID clientId, UUID providerId, UUID ruleId, String description, Instant expiresAt) {
        this.clientId = clientId;
        this.providerId = providerId;
        this.ruleId = ruleId;
        this.description = description;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        return status == RewardStatus.AVAILABLE && expiresAt.isAfter(Instant.now());
    }

    public boolean hasExpired() {
        return status == RewardStatus.AVAILABLE && !expiresAt.isAfter(Instant.now());
    }

    public void redeem(UUID reservationId) {
        this.status = RewardStatus.REDEEMED;
        this.redeemedAt = Instant.now();
        this.reservationId = reservationId;
    }

    public void release() {
        this.status = expiresAt.isAfter(Instant.now()) ? RewardStatus.AVAILABLE : RewardStatus.EXPIRED;
        this.redeemedAt = null;
        this.reservationId = null;
    }

    public void markExpired() {
        this.status = RewardStatus.EXPIRED;
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

    public UUID getRuleId() {
        return ruleId;
    }

    public String getDescription() {
        return description;
    }

    public RewardStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public UUID getReservationId() {
        return reservationId;
    }
}
