package com.booking.user.dto;

import jakarta.validation.constraints.NotNull;

/** Boolean, a ne dva endpointa: administrator istom rutom i ukida i vraća pristup. */
public record UpdateEnabledRequest(@NotNull Boolean enabled) {
}
