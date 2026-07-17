package com.booking.user.controller;

import com.booking.common.security.CurrentUser;
import com.booking.user.dto.UpdateRoleRequest;
import com.booking.user.dto.UserResponse;
import com.booking.user.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Korisnici")
public class UserController {

    private final UserAccountService service;

    public UserController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Profil trenutno autentifikovanog korisnika")
    public UserResponse me() {
        return service.findById(CurrentUser.requiredId());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Izlistaj sve korisnike (ADMIN)")
    public List<UserResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    @Operation(summary = "Preuzmi jednog korisnika (ADMIN ili sam korisnik)")
    public UserResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promeni ulogu korisnika (ADMIN)")
    public UserResponse updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return service.updateRole(id, request.role());
    }
}
