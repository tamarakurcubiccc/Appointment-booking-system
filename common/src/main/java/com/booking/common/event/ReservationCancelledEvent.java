package com.booking.common.event;

import java.time.Instant;
import java.util.UUID;

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
