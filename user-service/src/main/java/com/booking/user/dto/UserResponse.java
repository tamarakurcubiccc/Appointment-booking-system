package com.booking.user.dto;

import com.booking.user.entity.Role;
import com.booking.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getPhone(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
