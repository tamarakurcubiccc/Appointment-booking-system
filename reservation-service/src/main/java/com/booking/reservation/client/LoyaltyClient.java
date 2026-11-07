package com.booking.reservation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Korišćenje nagrade je sinhrono, iako brojanje termina ide preko događaja: ishod mora
 * da bude poznat dok zakazivanje traje, da se jedna nagrada ne bi iskoristila dva puta.
 */
@FeignClient(name = "loyalty-service", path = "/api/internal/loyalty")
public interface LoyaltyClient {

    @GetMapping("/rewards")
    List<LoyaltyRewardDto> usableRewards(@RequestParam UUID clientId, @RequestParam UUID providerId);

    @PostMapping("/rewards/redeem")
    LoyaltyRewardDto redeem(@RequestBody RedeemRewardRequest request);
}
