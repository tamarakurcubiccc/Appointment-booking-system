package com.booking.loyalty.service;

import com.booking.common.exception.ApiException;
import com.booking.loyalty.dto.RedeemRewardRequest;
import com.booking.loyalty.entity.LoyaltyAccount;
import com.booking.loyalty.entity.LoyaltyReward;
import com.booking.loyalty.entity.LoyaltyRule;
import com.booking.loyalty.entity.RewardStatus;
import com.booking.loyalty.repository.LoyaltyAccountRepository;
import com.booking.loyalty.repository.LoyaltyRewardRepository;
import com.booking.loyalty.repository.LoyaltyRuleRepository;
import com.booking.loyalty.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {
    @Mock
    private LoyaltyAccountRepository accounts;
    @Mock
    private LoyaltyRewardRepository rewards;
    @Mock
    private LoyaltyRuleRepository rules;
    @Mock
    private ProcessedEventRepository processedEvents;

    @InjectMocks
    private LoyaltyService service;

    private UUID clientId;
    private UUID providerId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    private LoyaltyRule rule(int required) {
        return new LoyaltyRule(providerId, required, "Besplatan termin", 90);
    }

    private LoyaltyAccount accountWith(int towardsReward) {
        var account = new LoyaltyAccount(clientId, providerId);
        for (int i = 0; i < towardsReward; i++) {
            account.registerAppointment();
        }
        return account;
    }

    @Test
    void countsAnAppointmentWithoutIssuingARewardBelowTheThreshold() {
        var account = accountWith(1);
        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(accounts.findByClientIdAndProviderId(clientId, providerId)).thenReturn(Optional.of(account));
        when(rules.findByProviderIdAndActiveTrue(providerId)).thenReturn(Optional.of(rule(3)));

        service.registerAppointment(eventId, clientId, providerId);

        assertThat(account.getAppointmentsTowardsReward()).isEqualTo(2);
        verify(rewards, never()).save(any());
    }

    @Test
    void issuesARewardWhenTheThresholdIsReached() {
        var account = accountWith(2);
        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(accounts.findByClientIdAndProviderId(clientId, providerId)).thenReturn(Optional.of(account));
        when(rules.findByProviderIdAndActiveTrue(providerId)).thenReturn(Optional.of(rule(3)));

        service.registerAppointment(eventId, clientId, providerId);

        verify(rewards).save(any(LoyaltyReward.class));

        assertThat(account.getAppointmentsTowardsReward()).isZero();
        assertThat(account.getTotalAppointments()).isEqualTo(3);
    }

    @Test
    void carriesTheRemainderOverToTheNextReward() {
        var account = accountWith(4);
        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(accounts.findByClientIdAndProviderId(clientId, providerId)).thenReturn(Optional.of(account));
        when(rules.findByProviderIdAndActiveTrue(providerId)).thenReturn(Optional.of(rule(3)));

        service.registerAppointment(eventId, clientId, providerId);

        assertThat(account.getAppointmentsTowardsReward()).isEqualTo(2);
    }

    @Test
    void ignoresARedeliveredEvent() {
        when(processedEvents.existsById(eventId)).thenReturn(true);

        service.registerAppointment(eventId, clientId, providerId);

        verify(accounts, never()).findByClientIdAndProviderId(any(), any());
        verify(processedEvents, never()).save(any());
    }

    @Test
    void countsNothingWhenTheProviderHasNoActiveRule() {
        var account = accountWith(9);
        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(accounts.findByClientIdAndProviderId(clientId, providerId)).thenReturn(Optional.of(account));
        when(rules.findByProviderIdAndActiveTrue(providerId)).thenReturn(Optional.empty());

        service.registerAppointment(eventId, clientId, providerId);

        assertThat(account.getAppointmentsTowardsReward()).isEqualTo(10);
        verify(rewards, never()).save(any());
    }

    @Test
    void cancellationDecrementsTheCounterAndReleasesTheReward() {
        var account = accountWith(2);
        var reservationId = UUID.randomUUID();
        var reward = new LoyaltyReward(clientId, providerId, UUID.randomUUID(), "Besplatan termin",
                Instant.now().plus(30, ChronoUnit.DAYS));
        reward.redeem(reservationId);

        when(processedEvents.existsById(eventId)).thenReturn(false);
        when(accounts.findByClientIdAndProviderId(clientId, providerId)).thenReturn(Optional.of(account));
        when(rewards.findByReservationId(reservationId)).thenReturn(Optional.of(reward));

        service.registerCancellation(eventId, clientId, providerId, reservationId);

        assertThat(account.getAppointmentsTowardsReward()).isEqualTo(1);
        assertThat(reward.getStatus()).isEqualTo(RewardStatus.AVAILABLE);
        assertThat(reward.getReservationId()).isNull();
    }

    @Test
    void redeemingMarksTheRewardUsed() {
        var reservationId = UUID.randomUUID();
        var reward = new LoyaltyReward(clientId, providerId, UUID.randomUUID(), "Besplatan termin",
                Instant.now().plus(30, ChronoUnit.DAYS));
        var rewardId = UUID.randomUUID();
        when(rewards.findById(rewardId)).thenReturn(Optional.of(reward));

        var response = service.redeem(new RedeemRewardRequest(rewardId, clientId, providerId, reservationId));

        assertThat(response.status()).isEqualTo(RewardStatus.REDEEMED);
        assertThat(reward.getReservationId()).isEqualTo(reservationId);
    }

    @Test
    void aRewardCannotBeRedeemedTwice() {
        var reward = new LoyaltyReward(clientId, providerId, UUID.randomUUID(), "Besplatan termin",
                Instant.now().plus(30, ChronoUnit.DAYS));
        reward.redeem(UUID.randomUUID());
        var rewardId = UUID.randomUUID();
        when(rewards.findById(rewardId)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> service.redeem(
                new RedeemRewardRequest(rewardId, clientId, providerId, UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void aRewardCannotBeRedeemedBySomeoneElse() {
        var reward = new LoyaltyReward(clientId, providerId, UUID.randomUUID(), "Besplatan termin",
                Instant.now().plus(30, ChronoUnit.DAYS));
        var rewardId = UUID.randomUUID();
        when(rewards.findById(rewardId)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> service.redeem(
                new RedeemRewardRequest(rewardId, UUID.randomUUID(), providerId, UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anExpiredRewardIsRefusedAndMarkedExpired() {
        var reward = new LoyaltyReward(clientId, providerId, UUID.randomUUID(), "Besplatan termin",
                Instant.now().minus(1, ChronoUnit.DAYS));
        var rewardId = UUID.randomUUID();
        when(rewards.findById(rewardId)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> service.redeem(
                new RedeemRewardRequest(rewardId, clientId, providerId, UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(reward.getStatus()).isEqualTo(RewardStatus.EXPIRED);
    }

    @Test
    void aRewardCannotBeUsedWithAnotherProvider() {
        var reward = new LoyaltyReward(clientId, providerId, UUID.randomUUID(), "Besplatan termin",
                Instant.now().plus(30, ChronoUnit.DAYS));
        var rewardId = UUID.randomUUID();
        when(rewards.findById(rewardId)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> service.redeem(
                new RedeemRewardRequest(rewardId, clientId, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
