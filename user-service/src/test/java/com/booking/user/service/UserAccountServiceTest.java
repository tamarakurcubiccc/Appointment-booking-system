package com.booking.user.service;

import com.booking.common.event.BookingEvents;
import com.booking.common.event.UserRegisteredEvent;
import com.booking.common.exception.ApiException;
import com.booking.user.dto.LoginRequest;
import com.booking.user.dto.PasswordResetConfirmRequest;
import com.booking.user.dto.PasswordResetRequest;
import com.booking.user.dto.RegisterRequest;
import com.booking.user.entity.AccountToken;
import com.booking.user.entity.Role;
import com.booking.user.entity.TokenType;
import com.booking.user.entity.User;
import com.booking.user.messaging.DomainEventPublisher;
import com.booking.user.repository.AccountTokenRepository;
import com.booking.user.repository.UserRepository;
import com.booking.user.security.JwtService;
import com.booking.user.security.TokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private AccountTokenRepository tokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private DomainEventPublisher events;

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(repository, tokenRepository, new BCryptPasswordEncoder(), jwtService,
                events, new TokenProperties(Duration.ofHours(24), Duration.ofHours(1)));
    }

    private User savedUser(String email) {
        return new User(email, new BCryptPasswordEncoder().encode("password123"),
                "Marko Marković", "+381601112233", Role.CLIENT);
    }

    private RegisterRequest registration() {
        return new RegisterRequest("marko@example.com", "password123", "Marko Marković",
                "+381601112233", Role.CLIENT);
    }

    @Test
    void registrationLeavesTheAccountInactiveAndPublishesTheActivationEvent() {
        when(repository.existsByEmailIgnoreCase("marko@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any(AccountToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.register(registration());

        assertThat(response.activated()).isFalse();

        var event = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(events).publish(eq(BookingEvents.USER_REGISTERED), event.capture());
        assertThat(event.getValue().email()).isEqualTo("marko@example.com");
        assertThat(event.getValue().activationToken()).isNotBlank();
    }

    @Test
    void registrationDoesNotHandOutAnAccessToken() {
        when(repository.existsByEmailIgnoreCase("marko@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any(AccountToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(registration());

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void anUnactivatedAccountCannotLogIn() {
        when(repository.findByEmailIgnoreCase("marko@example.com"))
                .thenReturn(Optional.of(savedUser("marko@example.com")));

        assertThatThrownBy(() -> service.login(new LoginRequest("marko@example.com", "password123")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void activationEnablesLoginAndReturnsAToken() {
        var user = savedUser("marko@example.com");
        var token = new AccountToken(UUID.randomUUID(), "raw-token", TokenType.ACTIVATION,
                Instant.now().plusSeconds(3600));

        when(tokenRepository.findByTokenAndType("raw-token", TokenType.ACTIVATION)).thenReturn(Optional.of(token));
        when(repository.findById(token.getUserId())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt");

        var response = service.activate("raw-token");

        assertThat(response.accessToken()).isEqualTo("jwt");
        assertThat(response.user().activated()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void anActivationTokenWorksOnlyOnce() {
        var token = new AccountToken(UUID.randomUUID(), "raw-token", TokenType.ACTIVATION,
                Instant.now().plusSeconds(3600));
        token.markUsed();
        when(tokenRepository.findByTokenAndType("raw-token", TokenType.ACTIVATION)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.activate("raw-token"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anExpiredActivationTokenIsRefused() {
        var token = new AccountToken(UUID.randomUUID(), "raw-token", TokenType.ACTIVATION,
                Instant.now().minusSeconds(60));
        when(tokenRepository.findByTokenAndType("raw-token", TokenType.ACTIVATION)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.activate("raw-token")).isInstanceOf(ApiException.class);
        verify(repository, never()).findById(any());
    }

    @Test
    void aPasswordResetRequestForAnUnknownEmailLooksIdenticalFromOutside() {
        when(repository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        // Ne baca grešku - inače bi se preko odgovora moglo nabrajati koje adrese postoje.
        service.requestPasswordReset(new PasswordResetRequest("nobody@example.com"));

        verify(events, never()).publish(any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resettingThePasswordReplacesTheHashAndConsumesTheToken() {
        var user = savedUser("marko@example.com");
        String oldHash = user.getPasswordHash();
        var token = new AccountToken(UUID.randomUUID(), "reset-token", TokenType.PASSWORD_RESET,
                Instant.now().plusSeconds(600));

        when(tokenRepository.findByTokenAndType("reset-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(repository.findById(token.getUserId())).thenReturn(Optional.of(user));

        service.resetPassword(new PasswordResetConfirmRequest("reset-token", "newPassword123"));

        assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(new BCryptPasswordEncoder().matches("newPassword123", user.getPasswordHash())).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void anAdminRoleCannotBeSelfAssignedAtRegistration() {
        when(repository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(new RegisterRequest("admin@example.com", "password123",
                "Admin", null, Role.ADMIN)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(repository, never()).save(any());
    }
}
