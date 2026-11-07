package com.booking.catalog.repository;

import com.booking.catalog.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    List<ServiceOffering> findByActiveTrue();

    List<ServiceOffering> findByProviderId(UUID providerId);
}
