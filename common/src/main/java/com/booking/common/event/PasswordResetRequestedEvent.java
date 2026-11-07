package com.booking.common.event;

import java.time.Instant;
import java.util.UUID;

/** Objavljuje ga user-service kada korisnik zatraži promenu lozinke. */
public record PasswordResetRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String email,
        String fullName,
        String resetToken
) {
}
