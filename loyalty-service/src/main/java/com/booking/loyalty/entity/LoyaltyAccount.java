package com.booking.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Brojač termina jednog klijenta kod jednog pružaoca usluge.
 *
 * <p>{@code appointmentsTowardsReward} se umanjuje kada se nagrada dodeli, pa termini
 * preko praga ne propadaju - klijent koji zakaže 12 termina uz pravilo "svaki 10." ima
 * posle nagrade 2 termina ka sledećoj. {@code totalAppointments} je informativan i
 * nikada se ne umanjuje zbog dodeljene nagrade.
 */
@Entity
@Table(name = "loyalty_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_client_provider",
                columnNames = {"client_id", "provider_id"}))
public class LoyaltyAccount {

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

    @Column(name = "appointments_towards_reward", nullable = false)
    private int appointmentsTowardsReward;

    @Column(name = "total_appointments", nullable = false)
    private int totalAppointments;

    /**
     * Dva termina istog klijenta zakazana u istom trenutku menjaju isti red. Optimističko
     * zaključavanje pretvara taj sudar u grešku koju RabbitMQ ponavlja, umesto da jedan
     * od dva termina tiho nestane iz brojača.
     */
    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected LoyaltyAccount() {
    }

    public LoyaltyAccount(UUID clientId, UUID providerId) {
        this.clientId = clientId;
        this.providerId = providerId;
    }

    public void registerAppointment() {
        this.appointmentsTowardsReward++;
        this.totalAppointments++;
        this.updatedAt = Instant.now();
    }

    /** Otkazan termin se ne računa. Brojač ne ide ispod nule. */
    public void registerCancellation() {
        if (this.appointmentsTowardsReward > 0) {
            this.appointmentsTowardsReward--;
        }
        if (this.totalAppointments > 0) {
            this.totalAppointments--;
        }
        this.updatedAt = Instant.now();
    }

    /** Poziva se kada je nagrada dodeljena - prag se "potroši", ostatak se prenosi. */
    public void consumeThreshold(int requiredAppointments) {
        this.appointmentsTowardsReward -= requiredAppointments;
        if (this.appointmentsTowardsReward < 0) {
            this.appointmentsTowardsReward = 0;
        }
        this.updatedAt = Instant.now();
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

    public int getAppointmentsTowardsReward() {
        return appointmentsTowardsReward;
    }

    public int getTotalAppointments() {
        return totalAppointments;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
