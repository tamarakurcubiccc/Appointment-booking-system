package com.booking.user.controller;

import com.booking.user.dto.AuthResponse;
import com.booking.user.dto.LoginRequest;
import com.booking.user.dto.RegisterRequest;
import com.booking.user.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autentikacija")
public class AuthController {

    private final UserAccountService service;

    public AuthController(UserAccountService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @Operation(summary = "Registruj nov nalog i vrati pristupni token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentifikuj se i vrati pristupni token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}
