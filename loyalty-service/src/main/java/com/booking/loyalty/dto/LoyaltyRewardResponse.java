package com.booking.loyalty.dto;

import com.booking.loyalty.entity.LoyaltyReward;
import com.booking.loyalty.entity.RewardStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * @param status   Zapisan status; nagrada kojoj je rok prošao prikazuje se kao EXPIRED
 *                 i bez pozadinskog posla koji bi menjao kolonu.
 * @param usable   Može li da se iskoristi u ovom trenutku.
 */
public record LoyaltyRewardResponse(
        UUID id,
        UUID clientId,
        UUID providerId,
        String description,
        RewardStatus status,
        boolean usable,
        Instant issuedAt,
        Instant expiresAt,
        Instant redeemedAt,
        UUID reservationId
) {
    public static LoyaltyRewardResponse from(LoyaltyReward reward) {
        RewardStatus status = reward.hasExpired() ? RewardStatus.EXPIRED : reward.getStatus();

        return new LoyaltyRewardResponse(reward.getId(), reward.getClientId(), reward.getProviderId(),
                reward.getDescription(), status, reward.isUsable(), reward.getIssuedAt(),
                reward.getExpiresAt(), reward.getRedeemedAt(), reward.getReservationId());
    }
}
