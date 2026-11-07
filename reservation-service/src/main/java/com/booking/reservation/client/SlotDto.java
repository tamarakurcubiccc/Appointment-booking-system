package com.booking.reservation.client;

import java.time.Instant;
import java.util.UUID;

public record SlotDto(
        UUID id,
        UUID serviceId,
        UUID providerId,
        Instant startTime,
        Instant endTime,
        String status
) {
    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }
}
