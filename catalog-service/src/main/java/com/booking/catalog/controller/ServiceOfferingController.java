package com.booking.catalog.controller;

import com.booking.common.security.CurrentUser;
import com.booking.catalog.dto.ServiceOfferingRequest;
import com.booking.catalog.dto.ServiceOfferingResponse;
import com.booking.catalog.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Usluge")
public class ServiceOfferingController {
    private final ServiceCatalogService service;

    public ServiceOfferingController(ServiceCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Izlistaj aktivne usluge, opciono filtrirane po pružaocu usluge")
    public List<ServiceOfferingResponse> findAll(@RequestParam(required = false) UUID providerId) {
        return providerId == null ? service.findAllActive() : service.findByProvider(providerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Preuzmi jednu uslugu")
    public ServiceOfferingResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Kreiraj uslugu (PROVIDER)")
    public ResponseEntity<ServiceOfferingResponse> create(@Valid @RequestBody ServiceOfferingRequest request) {
        var created = service.create(CurrentUser.requiredId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Izmeni uslugu (vlasnik ili ADMIN)")
    public ServiceOfferingResponse update(@PathVariable UUID id, @Valid @RequestBody ServiceOfferingRequest request) {
        return service.update(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Deaktiviraj uslugu (soft delete)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }
}
