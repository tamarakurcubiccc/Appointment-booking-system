package com.booking.loyalty.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoyaltyRuleRequest(

        /* Najmanje 2 - pravilo "svaki 1. termin je nagrađen" nije program lojalnosti. */
        @Min(2) @Max(100)
        int requiredAppointments,

        @NotBlank @Size(max = 200)
        String rewardDescription,

        @Min(1) @Max(730)
        int rewardValidityDays
) {
}
