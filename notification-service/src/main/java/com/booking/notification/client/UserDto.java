package com.booking.notification.client;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        String phone,
        String role
) {
}
