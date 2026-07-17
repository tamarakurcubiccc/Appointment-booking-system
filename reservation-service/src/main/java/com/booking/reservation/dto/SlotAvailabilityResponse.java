package com.booking.reservation.dto;

import java.util.UUID;

public record SlotAvailabilityResponse(
        UUID slotId,
        boolean available,
        String reason
) {
    public static SlotAvailabilityResponse free(UUID slotId) {
        return new SlotAvailabilityResponse(slotId, true, null);
    }

    public static SlotAvailabilityResponse taken(UUID slotId, String reason) {
        return new SlotAvailabilityResponse(slotId, false, reason);
    }
}
