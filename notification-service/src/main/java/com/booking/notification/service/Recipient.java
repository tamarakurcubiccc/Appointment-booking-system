package com.booking.notification.service;

import com.booking.notification.client.UserDto;

import java.util.UUID;

/**
 * Primalac obaveštenja.
 *
 * @param id Id korisnika iz user-service, ili null kada primalac nije korisnik sistema.
 */
public record Recipient(UUID id, String email, String fullName) {

    public static Recipient from(UserDto user) {
        return new Recipient(user.id(), user.email(), user.fullName());
    }
}
