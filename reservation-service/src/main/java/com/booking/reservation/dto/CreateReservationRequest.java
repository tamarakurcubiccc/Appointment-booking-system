package com.booking.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID serviceId,
        @NotNull UUID slotId,
        @Size(max = 500) String note
) {
}
