package com.booking.loyalty.service;

import com.booking.common.exception.ApiException;
import com.booking.loyalty.dto.LoyaltyAccountResponse;
import com.booking.loyalty.dto.LoyaltyRewardResponse;
import com.booking.loyalty.dto.RedeemRewardRequest;
import com.booking.loyalty.entity.LoyaltyAccount;
import com.booking.loyalty.entity.LoyaltyReward;
import com.booking.loyalty.entity.ProcessedEvent;
import com.booking.loyalty.entity.RewardStatus;
import com.booking.loyalty.repository.LoyaltyAccountRepository;
import com.booking.loyalty.repository.LoyaltyRewardRepository;
import com.booking.loyalty.repository.LoyaltyRuleRepository;
import com.booking.loyalty.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LoyaltyService {
    private static final Logger log = LoggerFactory.getLogger(LoyaltyService.class);

    private final LoyaltyAccountRepository accounts;
    private final LoyaltyRewardRepository rewards;
    private final LoyaltyRuleRepository rules;
    private final ProcessedEventRepository processedEvents;

    public LoyaltyService(LoyaltyAccountRepository accounts,
                          LoyaltyRewardRepository rewards,
                          LoyaltyRuleRepository rules,
                          ProcessedEventRepository processedEvents) {
        this.accounts = accounts;
        this.rewards = rewards;
        this.rules = rules;
        this.processedEvents = processedEvents;
    }

    @Transactional
    public void registerAppointment(UUID eventId, UUID clientId, UUID providerId) {
        if (alreadyProcessed(eventId, "reservation.created")) {
            return;
        }

        var account = accounts.findByClientIdAndProviderId(clientId, providerId)
                .orElseGet(() -> accounts.save(new LoyaltyAccount(clientId, providerId)));

        account.registerAppointment();
        issueRewardIfEarned(account);

        log.info("Client {} now has {} appointment(s) towards a reward with provider {}",
                clientId, account.getAppointmentsTowardsReward(), providerId);
    }

    @Transactional
    public void registerCancellation(UUID eventId, UUID clientId, UUID providerId, UUID reservationId) {
        if (alreadyProcessed(eventId, "reservation.cancelled")) {
            return;
        }

        accounts.findByClientIdAndProviderId(clientId, providerId)
                .ifPresent(LoyaltyAccount::registerCancellation);

        rewards.findByReservationId(reservationId).ifPresent(reward -> {
            reward.release();
            log.info("Reward {} released back to client {} after cancelling reservation {}",
                    reward.getId(), clientId, reservationId);
        });

        log.info("Cancellation of reservation {} registered for client {}", reservationId, clientId);
    }

    @Transactional
    public LoyaltyRewardResponse redeem(RedeemRewardRequest request) {
        var reward = rewards.findById(request.rewardId())
                .orElseThrow(() -> ApiException.notFound("Nagrada " + request.rewardId() + " nije pronađena"));

        if (!reward.getClientId().equals(request.clientId())) {
            throw ApiException.forbidden("Nagrada ne pripada ovom klijentu");
        }
        if (!reward.getProviderId().equals(request.providerId())) {
            throw ApiException.badRequest("Nagrada se može iskoristiti samo kod pružaoca koji ju je dodelio");
        }
        if (reward.getStatus() == RewardStatus.REDEEMED) {
            throw ApiException.conflict("Nagrada je već iskorišćena");
        }
        if (reward.hasExpired()) {
            reward.markExpired();
            throw ApiException.badRequest("Nagrada je istekla");
        }
        if (reward.getStatus() == RewardStatus.EXPIRED) {
            throw ApiException.badRequest("Nagrada je istekla");
        }

        reward.redeem(request.reservationId());
        log.info("Reward {} redeemed by client {} in reservation {}",
                reward.getId(), request.clientId(), request.reservationId());

        return LoyaltyRewardResponse.from(reward);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyRewardResponse> findUsableRewards(UUID clientId, UUID providerId) {
        return rewards.findByClientIdAndProviderIdAndStatusOrderByExpiresAtAsc(
                        clientId, providerId, RewardStatus.AVAILABLE).stream()
                .filter(LoyaltyReward::isUsable)
                .map(LoyaltyRewardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoyaltyRewardResponse> findMyRewards(UUID clientId) {
        return rewards.findByClientIdOrderByIssuedAtDesc(clientId).stream()
                .map(LoyaltyRewardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LoyaltyAccountResponse> findMyAccounts(UUID clientId) {
        return accounts.findByClientIdOrderByUpdatedAtDesc(clientId).stream()
                .map(account -> LoyaltyAccountResponse.from(account, activeThreshold(account.getProviderId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoyaltyAccountResponse> findProviderAccounts(UUID providerId) {
        Integer threshold = activeThreshold(providerId);
        return accounts.findByProviderIdOrderByTotalAppointmentsDesc(providerId).stream()
                .map(account -> LoyaltyAccountResponse.from(account, threshold))
                .toList();
    }

    private void issueRewardIfEarned(LoyaltyAccount account) {
        var rule = rules.findByProviderIdAndActiveTrue(account.getProviderId());
        if (rule.isEmpty()) {
            return;
        }

        var active = rule.get();
        if (account.getAppointmentsTowardsReward() < active.getRequiredAppointments()) {
            return;
        }

        var reward = new LoyaltyReward(account.getClientId(), account.getProviderId(), active.getId(),
                active.getRewardDescription(),
                Instant.now().plus(active.getRewardValidityDays(), ChronoUnit.DAYS));

        rewards.save(reward);
        account.consumeThreshold(active.getRequiredAppointments());

        log.info("Issued reward {} to client {} for provider {}",
                reward.getId(), account.getClientId(), account.getProviderId());
    }

    private Integer activeThreshold(UUID providerId) {
        return rules.findByProviderIdAndActiveTrue(providerId)
                .map(rule -> rule.getRequiredAppointments())
                .orElse(null);
    }

    private boolean alreadyProcessed(UUID eventId, String eventType) {
        if (processedEvents.existsById(eventId)) {
            log.debug("Event {} already processed - ignoring the redelivery", eventId);
            return true;
        }
        processedEvents.save(new ProcessedEvent(eventId, eventType));
        return false;
    }
}
