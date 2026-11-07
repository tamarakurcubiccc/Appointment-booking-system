package com.booking.user.entity;

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

/**
 * Jednokratni token za aktivaciju naloga ili promenu lozinke.
 *
 * <p>Zasebna tabela, a ne kolone na korisniku: token ima rok trajanja i smisao mu je da
 * se potroši, pa istorija ostaje vidljiva, a jedan korisnik može imati više zahteva
 * za promenu lozinke bez prepisivanja prethodnog.
 */
@Entity
@Table(
        name = "account_tokens",
        indexes = {
                @Index(name = "idx_account_token_value", columnList = "token", unique = true),
                @Index(name = "idx_account_token_user", columnList = "user_id")
        }
)
public class AccountToken {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AccountToken() {
    }

    public AccountToken(UUID userId, String token, TokenType type, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    /** Token vredi samo ako nije već potrošen i nije istekao. */
    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public TokenType getType() {
        return type;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
