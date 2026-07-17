package com.booking.catalog.dto;

import com.booking.catalog.entity.AvailabilitySlot;
import com.booking.catalog.entity.SlotStatus;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID serviceId,
        UUID providerId,
        Instant startTime,
        Instant endTime,
        SlotStatus status
) {
    public static SlotResponse from(AvailabilitySlot slot) {
        return new SlotResponse(slot.getId(), slot.getService().getId(), slot.getProviderId(),
                slot.getStartTime(), slot.getEndTime(), slot.getStatus());
    }
}
