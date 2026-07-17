package com.booking.user.controller;

import com.booking.user.dto.UserResponse;
import com.booking.user.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpointi za komunikaciju između servisa. Nisu izloženi kroz API Gateway.
 */
@RestController
@RequestMapping("/api/internal/users")
@Tag(name = "Interno")
public class InternalUserController {

    private final UserAccountService service;

    public InternalUserController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Preuzmi korisnika - koristi ga catalog-service i reservation-service")
    public UserResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }
}
