package com.booking.catalog.dto;

import com.booking.catalog.entity.ServiceOffering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceOfferingResponse(
        UUID id,
        UUID providerId,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        boolean active,
        Instant createdAt
) {
    public static ServiceOfferingResponse from(ServiceOffering s) {
        return new ServiceOfferingResponse(s.getId(), s.getProviderId(), s.getName(), s.getDescription(),
                s.getDurationMinutes(), s.getPrice(), s.isActive(), s.getCreatedAt());
    }
}
