package com.booking.reservation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "catalog-service", path = "/api/internal")
public interface CatalogClient {
    @GetMapping("/slots/{slotId}")
    SlotDto getSlot(@PathVariable UUID slotId);

    @PostMapping("/slots/{slotId}/book")
    SlotDto bookSlot(@PathVariable UUID slotId);

    @PostMapping("/slots/{slotId}/release")
    SlotDto releaseSlot(@PathVariable UUID slotId);
}
