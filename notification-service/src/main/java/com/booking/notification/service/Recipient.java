package com.booking.notification.service;

import com.booking.notification.client.UserDto;

import java.util.UUID;

public record Recipient(UUID id, String email, String fullName) {
    public static Recipient from(UserDto user) {
        return new Recipient(user.id(), user.email(), user.fullName());
    }
}
