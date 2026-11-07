package com.booking.common.event;

import java.time.Instant;
import java.util.UUID;

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
