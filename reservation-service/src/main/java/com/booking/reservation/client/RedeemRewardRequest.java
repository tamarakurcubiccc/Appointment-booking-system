package com.booking.reservation.client;

import java.util.UUID;

public record RedeemRewardRequest(
        UUID rewardId,
        UUID clientId,
        UUID providerId,
        UUID reservationId
) {
}
