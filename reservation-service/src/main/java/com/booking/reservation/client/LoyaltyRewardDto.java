package com.booking.reservation.client;

import java.util.UUID;

public record LoyaltyRewardDto(
        UUID id,
        UUID clientId,
        UUID providerId,
        String description,
        String status,
        boolean usable
) {
}
