package com.booking.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Objavljuje ga user-service nakon uspešne registracije.
 *
 * @param eventId          Jedinstven id događaja - potrošači ga koriste za idempotentnost.
 * @param activationToken  Token za aktivaciju naloga; notification-service od njega gradi link.
 */
public record UserRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String email,
        String fullName,
        String role,
        String activationToken
) {
}
