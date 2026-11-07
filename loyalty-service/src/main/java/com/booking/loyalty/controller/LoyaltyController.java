package com.booking.loyalty.controller;

import com.booking.common.security.CurrentUser;
import com.booking.loyalty.dto.LoyaltyAccountResponse;
import com.booking.loyalty.dto.LoyaltyRewardResponse;
import com.booking.loyalty.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@Tag(name = "Lojalnost")
public class LoyaltyController {
    private final LoyaltyService service;

    public LoyaltyController(LoyaltyService service) {
        this.service = service;
    }

    @GetMapping("/accounts/my")
    @Operation(summary = "Brojači termina trenutnog klijenta, po pružaocu usluge")
    public List<LoyaltyAccountResponse> myAccounts() {
        return service.findMyAccounts(CurrentUser.requiredId());
    }

    @GetMapping("/rewards/my")
    @Operation(summary = "Nagrade trenutnog klijenta")
    public List<LoyaltyRewardResponse> myRewards() {
        return service.findMyRewards(CurrentUser.requiredId());
    }

    @GetMapping("/accounts/provider")
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    @Operation(summary = "Brojači klijenata trenutnog pružaoca usluge (PROVIDER)")
    public List<LoyaltyAccountResponse> providerAccounts() {
        return service.findProviderAccounts(CurrentUser.requiredId());
    }
}
