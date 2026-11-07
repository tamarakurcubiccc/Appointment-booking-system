package com.booking.reservation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "catalog-service", contextId = "catalogPublicClient", path = "/api/services")
public interface CatalogPublicClient {
    @GetMapping("/{serviceId}")
    ServiceOfferingDto getService(@PathVariable UUID serviceId);
}
