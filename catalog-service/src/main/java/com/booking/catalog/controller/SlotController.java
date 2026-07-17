package com.booking.catalog.controller;

import com.booking.common.security.CurrentUser;
import com.booking.catalog.dto.CreateSlotRequest;
import com.booking.catalog.dto.GenerateSlotsRequest;
import com.booking.catalog.dto.SlotResponse;
import com.booking.catalog.entity.SlotStatus;
import com.booking.catalog.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Dostupnost")
public class SlotController {

    private final AvailabilityService service;

    public SlotController(AvailabilityService service) {
        this.service = service;
    }

    @GetMapping("/slots")
    @Operation(summary = "Pretraži termine za uslugu u vremenskom opsegu (podrazumevano: narednih 30 dana; izostavi status za sve)")
    public List<SlotResponse> search(
            @RequestParam UUID serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) SlotStatus status) {
        return service.search(serviceId, from, to, status);
    }

    @GetMapping("/slots/{id}")
    @Operation(summary = "Preuzmi jedan termin")
    public SlotResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping("/services/{serviceId}/slots")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Kreiraj jedan termin")
    public ResponseEntity<SlotResponse> create(@PathVariable UUID serviceId,
                                               @Valid @RequestBody CreateSlotRequest request) {
        var created = service.createSlot(serviceId, CurrentUser.requiredId(),
                CurrentUser.hasRole("ADMIN"), request.startTime());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/services/{serviceId}/slots/generate")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Masovno generiši termine za opseg datuma")
    public ResponseEntity<List<SlotResponse>> generate(@PathVariable UUID serviceId,
                                                       @Valid @RequestBody GenerateSlotsRequest request) {
        var created = service.generateSlots(serviceId, CurrentUser.requiredId(),
                CurrentUser.hasRole("ADMIN"), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/slots/{id}/status")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Blokiraj ili deblokiraj termin")
    public SlotResponse setStatus(@PathVariable UUID id, @RequestParam SlotStatus status) {
        return service.setStatus(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"), status);
    }

    @DeleteMapping("/slots/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Obriši slobodan termin")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }
}
