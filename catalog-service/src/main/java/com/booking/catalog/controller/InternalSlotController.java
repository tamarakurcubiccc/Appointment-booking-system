package com.booking.catalog.controller;

import com.booking.catalog.dto.SlotResponse;
import com.booking.catalog.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Poziva ga reservation-service preko REST-a (Eureka + OpenFeign).
 * Ne rutira se kroz API Gateway - dostupan je samo unutar mreže klastera.
 */
@RestController
@RequestMapping("/api/internal/slots")
@Tag(name = "Interno")
public class InternalSlotController {

    private final AvailabilityService service;

    public InternalSlotController(AvailabilityService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Preuzmi detalje termina")
    public SlotResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/book")
    @Operation(summary = "Atomično označi termin kao BOOKED (409 ako je zauzet)")
    public SlotResponse book(@PathVariable UUID id) {
        return service.book(id);
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Vrati termin u status AVAILABLE")
    public SlotResponse release(@PathVariable UUID id) {
        return service.release(id);
    }
}
