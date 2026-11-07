package com.booking.user.service;

import com.booking.common.event.BookingEvents;
import com.booking.common.event.PasswordResetRequestedEvent;
import com.booking.common.event.UserRegisteredEvent;
import com.booking.common.exception.ApiException;
import com.booking.user.dto.AuthResponse;
import com.booking.user.dto.LoginRequest;
import com.booking.user.dto.PasswordResetConfirmRequest;
import com.booking.user.dto.PasswordResetRequest;
import com.booking.user.dto.RegisterRequest;
import com.booking.user.dto.UserResponse;
import com.booking.user.entity.AccountToken;
import com.booking.user.entity.Role;
import com.booking.user.entity.TokenType;
import com.booking.user.entity.User;
import com.booking.user.messaging.DomainEventPublisher;
import com.booking.user.repository.AccountTokenRepository;
import com.booking.user.repository.UserRepository;
import com.booking.user.security.JwtService;
import com.booking.user.security.TokenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class UserAccountService {
    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository repository;
    private final AccountTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DomainEventPublisher events;
    private final TokenProperties tokenProperties;

    public UserAccountService(UserRepository repository,
                              AccountTokenRepository tokenRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService,
                              DomainEventPublisher events,
                              TokenProperties tokenProperties) {
        this.repository = repository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.events = events;
        this.tokenProperties = tokenProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("Nalog sa ovom e-mail adresom već postoji");
        }

        Role role = request.role() == null ? Role.CLIENT : request.role();
        if (role == Role.ADMIN) {
            throw ApiException.forbidden("ADMIN uloga ne može da se dodeli prilikom samostalne registracije");
        }

        var user = new User(
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone(),
                role);

        repository.save(user);

        var token = createToken(user.getId(), TokenType.ACTIVATION, tokenProperties.activationTtl());
        events.publish(BookingEvents.USER_REGISTERED, new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), user.getId(), user.getEmail(),
                user.getFullName(), user.getRole().name(), token.getToken()));

        log.info("Registered user {} with role {} - awaiting activation", user.getId(), user.getRole());
        return UserResponse.from(user);
    }

    @Transactional
    public AuthResponse activate(String token) {
        var accountToken = usableToken(token, TokenType.ACTIVATION, "Aktivacioni token");
        var user = repository.findById(accountToken.getUserId())
                .orElseThrow(() -> ApiException.notFound("Korisnik iz tokena nije pronađen"));

        accountToken.markUsed();
        user.activate();

        log.info("Activated account {}", user.getId());
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

        if (!user.isActivated()) {
            throw ApiException.forbidden("Nalog nije aktiviran - proverite aktivacioni link u e-mailu");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Nalog je deaktiviran");
        }

        return AuthResponse.bearer(jwtService.generateToken(user), jwtService.expirationSeconds(),
                UserResponse.from(user));
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        var existing = repository.findByEmailIgnoreCase(request.email());
        if (existing.isEmpty()) {
            log.info("Password reset requested for an unknown e-mail address");
            return;
        }

        var user = existing.get();
        var token = createToken(user.getId(), TokenType.PASSWORD_RESET, tokenProperties.passwordResetTtl());

        events.publish(BookingEvents.PASSWORD_RESET_REQUESTED, new PasswordResetRequestedEvent(
                UUID.randomUUID(), Instant.now(), user.getId(), user.getEmail(),
                user.getFullName(), token.getToken()));

        log.info("Password reset requested for user {}", user.getId());
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        var accountToken = usableToken(request.token(), TokenType.PASSWORD_RESET, "Token za promenu lozinke");
        var user = repository.findById(accountToken.getUserId())
                .orElseThrow(() -> ApiException.notFound("Korisnik iz tokena nije pronađen"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        accountToken.markUsed();

        log.info("Password changed for user {}", user.getId());
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

    @Transactional
    public UserResponse setEnabled(UUID id, boolean enabled, UUID callerId) {
        if (id.equals(callerId)) {
            throw ApiException.badRequest("Ne možete deaktivirati sopstveni nalog");
        }

        var user = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Korisnik " + id + " nije pronađen"));

        user.setEnabled(enabled);
        log.info("Account {} {} by {}", id, enabled ? "re-enabled" : "disabled", callerId);
        return UserResponse.from(user);
    }

    private AccountToken usableToken(String token, TokenType type, String label) {
        var accountToken = tokenRepository.findByTokenAndType(token, type)
                .orElseThrow(() -> ApiException.badRequest(label + " nije valjan"));

        if (!accountToken.isUsable()) {
            throw ApiException.badRequest(label + " je istekao ili je već iskorišćen");
        }
        return accountToken;
    }

    private AccountToken createToken(UUID userId, TokenType type, Duration ttl) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        return tokenRepository.save(new AccountToken(userId, value, type, Instant.now().plus(ttl)));
    }
}
