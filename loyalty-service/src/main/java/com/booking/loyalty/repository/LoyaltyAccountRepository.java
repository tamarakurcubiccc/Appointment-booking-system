package com.booking.loyalty.repository;

import com.booking.loyalty.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    Optional<LoyaltyAccount> findByClientIdAndProviderId(UUID clientId, UUID providerId);

    List<LoyaltyAccount> findByClientIdOrderByUpdatedAtDesc(UUID clientId);

    List<LoyaltyAccount> findByProviderIdOrderByTotalAppointmentsDesc(UUID providerId);
}
