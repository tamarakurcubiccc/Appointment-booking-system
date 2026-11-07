package com.booking.user.controller;

import com.booking.user.dto.AuthResponse;
import com.booking.user.dto.LoginRequest;
import com.booking.user.dto.PasswordResetConfirmRequest;
import com.booking.user.dto.PasswordResetRequest;
import com.booking.user.dto.RegisterRequest;
import com.booking.user.dto.UserResponse;
import com.booking.user.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "Registruj nov nalog; aktivacioni link se šalje na e-mail")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    /**
     * Link iz e-maila vodi na stranicu koja poziva ovaj endpoint. Aktivacija menja stanje
     * naloga, pa je POST, a ne GET - token iz linka se prosleđuje kao parametar.
     */
    @PostMapping("/activate")
    @Operation(summary = "Aktiviraj nalog tokenom iz e-maila i vrati pristupni token")
    public AuthResponse activate(@RequestParam String token) {
        return service.activate(token);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentifikuj se i vrati pristupni token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    /** 202 bez tela i kada e-mail adresa ne postoji - vidi UserAccountService. */
    @PostMapping("/password-reset-request")
    @Operation(summary = "Zatraži link za promenu lozinke")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        service.requestPasswordReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Postavi novu lozinku pomoću tokena iz e-maila")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        service.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
