package com.booking.loyalty.dto;

import com.booking.loyalty.entity.LoyaltyRule;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyRuleResponse(
        UUID id,
        UUID providerId,
        int requiredAppointments,
        String rewardDescription,
        int rewardValidityDays,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static LoyaltyRuleResponse from(LoyaltyRule rule) {
        return new LoyaltyRuleResponse(rule.getId(), rule.getProviderId(), rule.getRequiredAppointments(),
                rule.getRewardDescription(), rule.getRewardValidityDays(), rule.isActive(),
                rule.getCreatedAt(), rule.getUpdatedAt());
    }
}
