package com.booking.reservation.client;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        String phone,
        String role
) {
}
