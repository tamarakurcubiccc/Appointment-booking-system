package com.booking.reservation.service;

import com.booking.common.exception.ApiException;
import com.booking.reservation.client.CatalogClient;
import com.booking.reservation.client.CatalogPublicClient;
import com.booking.reservation.client.SlotDto;
import com.booking.reservation.client.UserClient;
import com.booking.reservation.client.UserDto;
import com.booking.reservation.dto.CreateReservationRequest;
import com.booking.reservation.dto.ReservationResponse;
import com.booking.reservation.dto.SlotAvailabilityResponse;
import com.booking.reservation.entity.Reservation;
import com.booking.reservation.entity.ReservationStatus;
import com.booking.reservation.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository repository;
    private final CatalogClient catalogClient;
    private final CatalogPublicClient catalogPublicClient;
    private final UserClient userClient;

    public ReservationService(ReservationRepository repository,
                              CatalogClient catalogClient,
                              CatalogPublicClient catalogPublicClient,
                              UserClient userClient) {
        this.repository = repository;
        this.catalogClient = catalogClient;
        this.catalogPublicClient = catalogPublicClient;
        this.userClient = userClient;
    }

    /**
     * Tok rezervacije:
     *   1. učitaj termin iz catalog-service i validiraj ga
     *   2. zatraži od catalog-service da ga atomično označi kao BOOKED (409 ako je neko bio brži)
     *   3. sačuvaj rezervaciju
     *   4. ako korak 3 ne uspe -> kompenzaciona akcija: vrati termin nazad
     *
     * Lokalna transakcija se otvara samo oko koraka 3 kako spor REST poziv
     * nikada ne bi držao otvorenu konekciju ka bazi.
     */
    public ReservationResponse book(UUID clientId, CreateReservationRequest request) {
        SlotDto slot = catalogClient.getSlot(request.slotId());

        if (!slot.serviceId().equals(request.serviceId())) {
            throw ApiException.badRequest("Termin " + slot.id() + " ne pripada usluzi " + request.serviceId());
        }
        if (!slot.isAvailable()) {
            throw ApiException.conflict("Termin nije slobodan (status: " + slot.status() + ")");
        }
        if (!slot.startTime().isAfter(Instant.now())) {
            throw ApiException.badRequest("Termin u prošlosti ne može da se zakaže");
        }
        if (repository.existsBySlotIdAndStatus(request.slotId(), ReservationStatus.CONFIRMED)) {
            throw ApiException.conflict("Za ovaj termin već postoji aktivna rezervacija");
        }

        var offering = catalogPublicClient.getService(request.serviceId());
        if (!offering.active()) {
            throw ApiException.badRequest("Usluga više nije aktivna");
        }

        // Korak 2 - jedini izvor istine za "ko je dobio termin".
        catalogClient.bookSlot(request.slotId());

        try {
            var reservation = new Reservation(clientId, slot.providerId(), slot.serviceId(), slot.id(),
                    offering.name(), slot.startTime(), slot.endTime(), request.note());
            repository.save(reservation);
            log.info("Client {} booked slot {} (reservation {})", clientId, slot.id(), reservation.getId());
            return ReservationResponse.from(reservation);
        } catch (RuntimeException ex) {
            log.error("Persisting the reservation failed - releasing slot {}", request.slotId(), ex);
            safeRelease(request.slotId());
            throw ex;
        }
    }

    @Transactional
    public ReservationResponse cancel(UUID reservationId, UUID callerId, boolean admin) {
        var reservation = repository.findById(reservationId)
                .orElseThrow(() -> ApiException.notFound("Rezervacija " + reservationId + " nije pronađena"));

        boolean isParticipant = reservation.getClientId().equals(callerId)
                || reservation.getProviderId().equals(callerId);
        if (!admin && !isParticipant) {
            throw ApiException.forbidden("Ne možete otkazati tuđu rezervaciju");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ApiException.conflict("Rezervacija je već otkazana");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw ApiException.conflict("Završena rezervacija ne može da se otkaže");
        }

        reservation.cancel();

        // Oslobodi termin u catalog-u tek NAKON što je ovo otkazivanje trajno komitovano.
        // Oslobađanje pre commit-a rizikuje da termin pređe u AVAILABLE dok rollback ostavlja
        // rezervaciju u CONFIRMED - tj. prozor za dvostruku rezervaciju.
        UUID slotId = reservation.getSlotId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeRelease(slotId);
                }
            });
        } else {
            safeRelease(slotId);
        }

        log.info("Reservation {} cancelled by {}", reservationId, callerId);
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findMine(UUID clientId, Pageable pageable) {
        return repository.findByClientIdOrderByStartTimeDesc(clientId, pageable).map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findForProvider(UUID providerId, Pageable pageable) {
        return repository.findByProviderIdOrderByStartTimeDesc(providerId, pageable).map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(UUID id, UUID callerId, boolean admin) {
        var reservation = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Rezervacija " + id + " nije pronađena"));

        boolean isParticipant = reservation.getClientId().equals(callerId)
                || reservation.getProviderId().equals(callerId);
        if (!admin && !isParticipant) {
            throw ApiException.forbidden("Pristup nije dozvoljen");
        }
        return ReservationResponse.from(reservation);
    }

    /**
     * Kontakt podaci klijenta koji je napravio rezervaciju - za prikaz rasporeda pružaocu usluge.
     * Podaci se nalaze u user-service, pa se dohvataju preko REST-a umesto da se ovde dupliraju.
     */
    public UserDto clientDetails(UUID reservationId, UUID callerId, boolean admin) {
        var reservation = repository.findById(reservationId)
                .orElseThrow(() -> ApiException.notFound("Rezervacija " + reservationId + " nije pronađena"));

        if (!admin && !reservation.getProviderId().equals(callerId)) {
            throw ApiException.forbidden("Samo pružalac usluge iz ove rezervacije može da vidi podatke o klijentu");
        }
        return userClient.getUser(reservation.getClientId());
    }

    /** Jeftina pred-provera koju UI može pozvati pre prikazivanja dugmeta "Book". */
    public SlotAvailabilityResponse checkAvailability(UUID slotId) {
        SlotDto slot = catalogClient.getSlot(slotId);

        if (!slot.isAvailable()) {
            return SlotAvailabilityResponse.taken(slotId, "Status termina je " + slot.status());
        }
        if (!slot.startTime().isAfter(Instant.now())) {
            return SlotAvailabilityResponse.taken(slotId, "Termin je u prošlosti");
        }
        if (repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)) {
            return SlotAvailabilityResponse.taken(slotId, "Za ovaj termin već postoji aktivna rezervacija");
        }
        return SlotAvailabilityResponse.free(slotId);
    }

    private void safeRelease(UUID slotId) {
        try {
            catalogClient.releaseSlot(slotId);
        } catch (RuntimeException ex) {
            // Ne obaraj otkazivanje zbog ovoga - zakazani job za usklađivanje
            // bi pokupio zaostali termin (vidi README - Known trade-offs).
            log.error("Failed to release slot {} - manual reconciliation may be needed", slotId, ex);
        }
    }
}
