package com.booking.reservation.client;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceOfferingDto(
        UUID id,
        UUID providerId,
        String name,
        int durationMinutes,
        BigDecimal price,
        boolean active
) {
}
