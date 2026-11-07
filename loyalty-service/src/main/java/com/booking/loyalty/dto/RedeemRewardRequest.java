package com.booking.loyalty.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RedeemRewardRequest(
        @NotNull UUID rewardId,
        @NotNull UUID clientId,
        @NotNull UUID providerId,
        @NotNull UUID reservationId
) {
}
