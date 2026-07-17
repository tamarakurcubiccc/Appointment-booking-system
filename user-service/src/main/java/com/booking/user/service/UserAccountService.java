package com.booking.user.service;

import com.booking.common.exception.ApiException;
import com.booking.user.dto.AuthResponse;
import com.booking.user.dto.LoginRequest;
import com.booking.user.dto.RegisterRequest;
import com.booking.user.dto.UserResponse;
import com.booking.user.entity.Role;
import com.booking.user.entity.User;
import com.booking.user.repository.UserRepository;
import com.booking.user.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserAccountService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("Nalog sa ovom e-mail adresom već postoji");
        }

        Role role = request.role() == null ? Role.CLIENT : request.role();
        if (role == Role.ADMIN) {
            // ADMIN ulogu dodeljuje samo drugi ADMIN kroz PUT /api/users/{id}/role
            throw ApiException.forbidden("ADMIN uloga ne može da se dodeli prilikom samostalne registracije");
        }

        var user = new User(
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone(),
                role);

        repository.save(user);
        log.info("Registered user {} with role {}", user.getId(), user.getRole());

        return AuthResponse.bearer(jwtService.generateToken(user), jwtService.expirationSeconds(),
                UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var user = repository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Pogrešna e-mail adresa ili lozinka"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Pogrešna e-mail adresa ili lozinka");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Nalog je deaktiviran");
        }

        return AuthResponse.bearer(jwtService.generateToken(user), jwtService.expirationSeconds(),
                UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return repository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("Korisnik " + id + " nije pronađen"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateRole(UUID id, Role role) {
        var user = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Korisnik " + id + " nije pronađen"));
        user.setRole(role);
        log.info("Role of user {} changed to {}", id, role);
        return UserResponse.from(user);
    }
}
