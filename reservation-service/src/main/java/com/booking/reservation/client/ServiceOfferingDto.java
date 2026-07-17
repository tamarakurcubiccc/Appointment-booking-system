package com.booking.reservation.client;

import java.math.BigDecimal;
import java.util.UUID;

/** Preslikavanje ServiceOfferingResponse iz catalog-service (samo polja koja su nam potrebna). */
public record ServiceOfferingDto(
        UUID id,
        UUID providerId,
        String name,
        int durationMinutes,
        BigDecimal price,
        boolean active
) {
}
