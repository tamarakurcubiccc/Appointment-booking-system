package com.booking.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Kontakt podatke primaoca drži user-service. Događaji nose samo id-jeve, pa se
 * e-mail adresa dohvata u trenutku slanja - tako poruke u redu ne nose lične podatke
 * i izmena e-mail adrese se odmah vidi.
 */
@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable UUID userId);
}
