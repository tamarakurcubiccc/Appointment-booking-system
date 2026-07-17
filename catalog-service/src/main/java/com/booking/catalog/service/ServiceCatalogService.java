package com.booking.catalog.service;

import com.booking.common.exception.ApiException;
import com.booking.catalog.dto.ServiceOfferingRequest;
import com.booking.catalog.dto.ServiceOfferingResponse;
import com.booking.catalog.entity.ServiceOffering;
import com.booking.catalog.repository.ServiceOfferingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServiceCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogService.class);

    private final ServiceOfferingRepository repository;

    public ServiceCatalogService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ServiceOfferingResponse create(UUID providerId, ServiceOfferingRequest request) {
        var offering = new ServiceOffering(providerId, request.name(), request.description(),
                request.durationMinutes(), request.price());
        repository.save(offering);
        log.info("Provider {} created service {}", providerId, offering.getId());
        return ServiceOfferingResponse.from(offering);
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> findAllActive() {
        return repository.findByActiveTrue().stream().map(ServiceOfferingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> findByProvider(UUID providerId) {
        return repository.findByProviderId(providerId).stream().map(ServiceOfferingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceOfferingResponse findById(UUID id) {
        return ServiceOfferingResponse.from(getOrThrow(id));
    }

    @Transactional
    public ServiceOfferingResponse update(UUID id, UUID callerId, boolean admin, ServiceOfferingRequest request) {
        var offering = getOrThrow(id);
        assertOwner(offering, callerId, admin);

        offering.setName(request.name());
        offering.setDescription(request.description());
        offering.setDurationMinutes(request.durationMinutes());
        offering.setPrice(request.price());
        return ServiceOfferingResponse.from(offering);
    }

    /** Meko brisanje - istorijske rezervacije moraju i dalje da pokazuju na validnu uslugu. */
    @Transactional
    public void deactivate(UUID id, UUID callerId, boolean admin) {
        var offering = getOrThrow(id);
        assertOwner(offering, callerId, admin);
        offering.setActive(false);
        log.info("Service {} deactivated", id);
    }

    @Transactional(readOnly = true)
    public ServiceOffering getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usluga " + id + " nije pronađena"));
    }

    public void assertOwner(ServiceOffering offering, UUID callerId, boolean admin) {
        if (!admin && !offering.getProviderId().equals(callerId)) {
            throw ApiException.forbidden("Niste vlasnik ove usluge");
        }
    }
}
