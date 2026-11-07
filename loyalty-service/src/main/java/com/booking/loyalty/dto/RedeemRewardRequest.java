package com.booking.loyalty.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Zahtev koji šalje reservation-service pri zakazivanju. Klijent i pružalac se prosleđuju
 * eksplicitno da bi se proverilo da nagrada pripada tom klijentu i tom pružaocu - id
 * nagrade sam po sebi ne dokazuje čija je.
 */
public record RedeemRewardRequest(
        @NotNull UUID rewardId,
        @NotNull UUID clientId,
        @NotNull UUID providerId,
        @NotNull UUID reservationId
) {
}
