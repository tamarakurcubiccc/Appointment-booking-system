package com.booking.loyalty.dto;

import com.booking.loyalty.entity.LoyaltyAccount;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyAccountResponse(
        UUID clientId,
        UUID providerId,
        int appointmentsTowardsReward,
        int totalAppointments,
        Integer requiredAppointments,
        Integer appointmentsUntilReward,
        Instant updatedAt
) {
    public static LoyaltyAccountResponse from(LoyaltyAccount account, Integer requiredAppointments) {
        Integer remaining = requiredAppointments == null
                ? null
                : Math.max(0, requiredAppointments - account.getAppointmentsTowardsReward());

        return new LoyaltyAccountResponse(account.getClientId(), account.getProviderId(),
                account.getAppointmentsTowardsReward(), account.getTotalAppointments(),
                requiredAppointments, remaining, account.getUpdatedAt());
    }
}
