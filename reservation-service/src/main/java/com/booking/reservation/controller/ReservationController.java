package com.booking.reservation.controller;

import com.booking.common.security.CurrentUser;
import com.booking.reservation.dto.CreateReservationRequest;
import com.booking.reservation.dto.ReservationResponse;
import com.booking.reservation.client.UserDto;
import com.booking.reservation.dto.SlotAvailabilityResponse;
import com.booking.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Rezervacije")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Rezerviši termin")
    public ResponseEntity<ReservationResponse> book(@Valid @RequestBody CreateReservationRequest request) {
        var created = service.book(CurrentUser.requiredId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Otkaži rezervaciju i oslobodi termin")
    public ReservationResponse cancel(@PathVariable UUID id) {
        return service.cancel(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
    }

    @GetMapping("/my")
    @Operation(summary = "Rezervacije trenutnog klijenta")
    public Page<ReservationResponse> my(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.findMine(CurrentUser.requiredId(), PageRequest.of(page, size));
    }

    @GetMapping("/provider")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Rezervacije zakazane kod trenutnog pružaoca usluge")
    public Page<ReservationResponse> forProvider(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.findForProvider(CurrentUser.requiredId(), PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Preuzmi jednu rezervaciju")
    public ReservationResponse findById(@PathVariable UUID id) {
        return service.findById(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
    }

    @GetMapping("/{id}/client")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Kontakt podaci klijenta koji je napravio rezervaciju (preuzeto iz user-service)")
    public UserDto clientDetails(@PathVariable UUID id) {
        return service.clientDetails(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
    }

    @GetMapping("/availability")
    @Operation(summary = "Proveri da li termin još može da se rezerviše")
    public SlotAvailabilityResponse availability(@RequestParam UUID slotId) {
        return service.checkAvailability(slotId);
    }
}
