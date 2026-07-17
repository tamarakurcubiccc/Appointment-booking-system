package com.booking.reservation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable UUID userId);
}
