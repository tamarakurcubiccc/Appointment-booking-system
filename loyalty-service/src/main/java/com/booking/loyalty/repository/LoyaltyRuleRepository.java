package com.booking.loyalty.repository;

import com.booking.loyalty.entity.LoyaltyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyRuleRepository extends JpaRepository<LoyaltyRule, UUID> {

    Optional<LoyaltyRule> findByProviderIdAndActiveTrue(UUID providerId);

    List<LoyaltyRule> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    List<LoyaltyRule> findByActiveTrueOrderByCreatedAtDesc();
}
