package com.booking.loyalty.controller;

import com.booking.loyalty.dto.LoyaltyRewardResponse;
import com.booking.loyalty.dto.RedeemRewardRequest;
import com.booking.loyalty.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpointi za komunikaciju između servisa. Nisu izloženi kroz API Gateway.
 *
 * <p>Korišćenje nagrade je sinhrono, za razliku od brojanja termina: ishod mora da bude
 * poznat dok zakazivanje još traje, da se nagrada ne bi iskoristila dva puta.
 */
@RestController
@RequestMapping("/api/internal/loyalty")
@Tag(name = "Interno")
public class InternalLoyaltyController {

    private final LoyaltyService service;

    public InternalLoyaltyController(LoyaltyService service) {
        this.service = service;
    }

    @GetMapping("/rewards")
    @Operation(summary = "Nagrade koje klijent trenutno može da iskoristi kod pružaoca usluge")
    public List<LoyaltyRewardResponse> usableRewards(@RequestParam UUID clientId,
                                                     @RequestParam UUID providerId) {
        return service.findUsableRewards(clientId, providerId);
    }

    @PostMapping("/rewards/redeem")
    @Operation(summary = "Iskoristi nagradu u rezervaciji - koristi ga reservation-service")
    public LoyaltyRewardResponse redeem(@Valid @RequestBody RedeemRewardRequest request) {
        return service.redeem(request);
    }
}
