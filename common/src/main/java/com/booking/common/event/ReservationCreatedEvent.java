package com.booking.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Objavljuje ga reservation-service nakon što je rezervacija trajno sačuvana.
 * Nosi samo id-jeve i činjenice o terminu - kontakt podatke potrošač dohvata
 * iz user-service, da se e-mail adrese ne bi duplirale kroz poruke.
 */
public record ReservationCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID reservationId,
        UUID clientId,
        UUID providerId,
        UUID serviceId,
        String serviceName,
        Instant startTime,
        Instant endTime
) {
}
