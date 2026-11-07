package com.booking.loyalty.service;

import com.booking.common.exception.ApiException;
import com.booking.loyalty.dto.LoyaltyRuleRequest;
import com.booking.loyalty.dto.LoyaltyRuleResponse;
import com.booking.loyalty.entity.LoyaltyRule;
import com.booking.loyalty.repository.LoyaltyRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LoyaltyRuleService {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyRuleService.class);

    private final LoyaltyRuleRepository repository;

    public LoyaltyRuleService(LoyaltyRuleRepository repository) {
        this.repository = repository;
    }

    /**
     * Pružalac može imati najviše jedno aktivno pravilo. Da bi promenio prag, prvo
     * deaktivira postojeće - tako se nikada ne postavlja pitanje koje od dva pravila
     * se primenjuje na jedan brojač.
     */
    @Transactional
    public LoyaltyRuleResponse create(UUID providerId, LoyaltyRuleRequest request) {
        if (repository.findByProviderIdAndActiveTrue(providerId).isPresent()) {
            throw ApiException.conflict("Već imate aktivno pravilo lojalnosti - deaktivirajte ga pre novog");
        }

        var rule = new LoyaltyRule(providerId, request.requiredAppointments(),
                request.rewardDescription(), request.rewardValidityDays());

        repository.save(rule);
        log.info("Provider {} created loyalty rule {} (every {} appointments)",
                providerId, rule.getId(), rule.getRequiredAppointments());
        return LoyaltyRuleResponse.from(rule);
    }

    @Transactional
    public LoyaltyRuleResponse update(UUID id, UUID callerId, boolean admin, LoyaltyRuleRequest request) {
        var rule = getOrThrow(id);
        assertOwner(rule, callerId, admin);

        rule.setRequiredAppointments(request.requiredAppointments());
        rule.setRewardDescription(request.rewardDescription());
        rule.setRewardValidityDays(request.rewardValidityDays());
        rule.markUpdated();

        log.info("Loyalty rule {} updated", id);
        return LoyaltyRuleResponse.from(rule);
    }

    /**
     * Deaktiviranje, a ne brisanje: već dodeljene nagrade pokazuju na pravilo po kojem su
     * nastale, pa red mora da ostane.
     */
    @Transactional
    public LoyaltyRuleResponse deactivate(UUID id, UUID callerId, boolean admin) {
        var rule = getOrThrow(id);
        assertOwner(rule, callerId, admin);

        rule.setActive(false);
        rule.markUpdated();

        log.info("Loyalty rule {} deactivated", id);
        return LoyaltyRuleResponse.from(rule);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyRuleResponse> findAllActive() {
        return repository.findByActiveTrueOrderByCreatedAtDesc().stream().map(LoyaltyRuleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LoyaltyRuleResponse> findByProvider(UUID providerId) {
        return repository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(LoyaltyRuleResponse::from).toList();
    }

    private LoyaltyRule getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Pravilo lojalnosti " + id + " nije pronađeno"));
    }

    private void assertOwner(LoyaltyRule rule, UUID callerId, boolean admin) {
        if (!admin && !rule.getProviderId().equals(callerId)) {
            throw ApiException.forbidden("Niste vlasnik ovog pravila lojalnosti");
        }
    }
}
