package com.booking.loyalty.repository;

import com.booking.loyalty.entity.LoyaltyReward;
import com.booking.loyalty.entity.RewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyRewardRepository extends JpaRepository<LoyaltyReward, UUID> {

    List<LoyaltyReward> findByClientIdOrderByIssuedAtDesc(UUID clientId);

    List<LoyaltyReward> findByClientIdAndProviderIdAndStatusOrderByExpiresAtAsc(
            UUID clientId, UUID providerId, RewardStatus status);

    Optional<LoyaltyReward> findByReservationId(UUID reservationId);
}
