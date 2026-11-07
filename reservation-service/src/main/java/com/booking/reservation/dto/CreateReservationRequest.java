package com.booking.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param rewardId Opciono - nagrada lojalnosti koja se koristi za ovaj termin. Mora da
 *                 pripada klijentu i pružaocu usluge kod kojeg se zakazuje.
 */
public record CreateReservationRequest(
        @NotNull UUID serviceId,
        @NotNull UUID slotId,
        @Size(max = 500) String note,
        UUID rewardId
) {
}
