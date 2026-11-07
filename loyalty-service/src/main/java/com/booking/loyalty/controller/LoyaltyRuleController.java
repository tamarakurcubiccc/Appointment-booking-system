package com.booking.loyalty.controller;

import com.booking.common.security.CurrentUser;
import com.booking.loyalty.dto.LoyaltyRuleRequest;
import com.booking.loyalty.dto.LoyaltyRuleResponse;
import com.booking.loyalty.service.LoyaltyRuleService;
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
@RequestMapping("/api/loyalty/rules")
@Tag(name = "Pravila lojalnosti")
public class LoyaltyRuleController {

    private final LoyaltyRuleService service;

    public LoyaltyRuleController(LoyaltyRuleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Izlistaj aktivna pravila, opciono po pružaocu usluge")
    public List<LoyaltyRuleResponse> findAll(@RequestParam(required = false) UUID providerId) {
        return providerId == null ? service.findAllActive() : service.findByProvider(providerId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Definiši pravilo lojalnosti (PROVIDER)")
    public ResponseEntity<LoyaltyRuleResponse> create(@Valid @RequestBody LoyaltyRuleRequest request) {
        var created = service.create(CurrentUser.requiredId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Izmeni pravilo lojalnosti (vlasnik ili ADMIN)")
    public LoyaltyRuleResponse update(@PathVariable UUID id, @Valid @RequestBody LoyaltyRuleRequest request) {
        return service.update(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"), request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Deaktiviraj pravilo lojalnosti")
    public LoyaltyRuleResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id, CurrentUser.requiredId(), CurrentUser.hasRole("ADMIN"));
    }
}
