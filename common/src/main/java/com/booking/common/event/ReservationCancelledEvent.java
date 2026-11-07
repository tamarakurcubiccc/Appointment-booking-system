package com.booking.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Objavljuje ga reservation-service nakon otkazivanja.
 *
 * @param cancelledBy Id korisnika koji je otkazao - klijent ili pružalac usluge.
 */
public record ReservationCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        UUID reservationId,
        UUID clientId,
        UUID providerId,
        String serviceName,
        Instant startTime,
        UUID cancelledBy
) {
}
