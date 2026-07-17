package com.booking.catalog.service;

import com.booking.common.exception.ApiException;
import com.booking.catalog.dto.GenerateSlotsRequest;
import com.booking.catalog.dto.SlotResponse;
import com.booking.catalog.entity.AvailabilitySlot;
import com.booking.catalog.entity.ServiceOffering;
import com.booking.catalog.entity.SlotStatus;
import com.booking.catalog.repository.AvailabilitySlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);

    private final AvailabilitySlotRepository slotRepository;
    private final ServiceCatalogService catalogService;

    public AvailabilityService(AvailabilitySlotRepository slotRepository, ServiceCatalogService catalogService) {
        this.slotRepository = slotRepository;
        this.catalogService = catalogService;
    }

    @Transactional
    public SlotResponse createSlot(UUID serviceId, UUID callerId, boolean admin, Instant startTime) {
        ServiceOffering offering = catalogService.getOrThrow(serviceId);
        catalogService.assertOwner(offering, callerId, admin);

        if (slotRepository.existsByServiceIdAndStartTime(serviceId, startTime)) {
            throw ApiException.conflict("Termin u " + startTime + " već postoji");
        }

        var slot = new AvailabilitySlot(offering, startTime, startTime.plusSeconds(offering.getDurationMinutes() * 60L));
        slotRepository.save(slot);
        return SlotResponse.from(slot);
    }

    /**
     * Masovno generiše termine za opseg datuma. Postojeći termini se preskaču, pa je
     * operacija bezbedna za ponovno pokretanje (idempotentna po vremenu početka).
     */
    @Transactional
    public List<SlotResponse> generateSlots(UUID serviceId, UUID callerId, boolean admin, GenerateSlotsRequest request) {
        ServiceOffering offering = catalogService.getOrThrow(serviceId);
        catalogService.assertOwner(offering, callerId, admin);

        if (request.toDate().isBefore(request.fromDate())) {
            throw ApiException.badRequest("Krajnji datum ne sme biti pre početnog datuma");
        }
        if (!request.workDayStart().isBefore(request.workDayEnd())) {
            throw ApiException.badRequest("Početak radnog dana mora biti pre kraja radnog dana");
        }

        ZoneId zone = request.zoneId() == null ? ZoneId.of("Europe/Belgrade") : ZoneId.of(request.zoneId());
        int duration = offering.getDurationMinutes();

        Instant rangeStart = request.fromDate().atStartOfDay(zone).toInstant();
        Instant rangeEnd = request.toDate().plusDays(1).atStartOfDay(zone).toInstant();
        // Jedan query umesto jednog po svakom kandidatu za termin.
        Set<Instant> existing = new HashSet<>(slotRepository.findStartTimes(serviceId, rangeStart, rangeEnd));

        List<AvailabilitySlot> created = new ArrayList<>();
        Instant now = Instant.now();
        long durationSeconds = duration * 60L;

        for (LocalDate date = request.fromDate(); !date.isAfter(request.toDate()); date = date.plusDays(1)) {
            if (request.skipWeekend() && isWeekend(date)) {
                continue;
            }
            // Granice radnog dana se jednom razreše u prave Instant vrednosti, pa se zatim
            // korača napred u Instant prostoru. Iteriranje po zidnom času (LocalDateTime) i
            // konverzija svakog kursora bi tiho izgubila termine pri DST pomeranju unapred
            // (dva lokalna vremena mapiraju se na jedan Instant) i ostavila prazninu pri
            // pomeranju unazad; korak fiksnog stvarnog trajanja čuva svaki termin tačno
            // `duration` minuta i daje ispravan broj termina na danu od 23h/25h.
            Instant dayStart = LocalDateTime.of(date, request.workDayStart()).atZone(zone).toInstant();
            Instant dayEnd = LocalDateTime.of(date, request.workDayEnd()).atZone(zone).toInstant();

            for (Instant start = dayStart;
                 !start.plusSeconds(durationSeconds).isAfter(dayEnd);
                 start = start.plusSeconds(durationSeconds)) {
                if (start.isAfter(now) && existing.add(start)) {
                    created.add(new AvailabilitySlot(offering, start, start.plusSeconds(durationSeconds)));
                }
            }
        }

        slotRepository.saveAll(created);
        log.info("Generated {} slots for service {}", created.size(), serviceId);
        return created.stream().map(SlotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> search(UUID serviceId, Instant from, Instant to, SlotStatus status) {
        Instant effectiveFrom = from == null ? Instant.now() : from;
        Instant effectiveTo = to == null ? effectiveFrom.plusSeconds(30L * 24 * 3600) : to;
        var slots = (status == null)
                ? slotRepository.search(serviceId, effectiveFrom, effectiveTo)
                : slotRepository.searchByStatus(serviceId, effectiveFrom, effectiveTo, status);

        return slots.stream().map(SlotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SlotResponse findById(UUID id) {
        return SlotResponse.from(getOrThrow(id));
    }

    @Transactional
    public SlotResponse setStatus(UUID slotId, UUID callerId, boolean admin, SlotStatus status) {
        var slot = getOrThrow(slotId);
        if (!admin && !slot.getProviderId().equals(callerId)) {
            throw ApiException.forbidden("Niste vlasnik ovog termina");
        }
        // Ovaj endpoint samo blokira/deblokira termin. BOOKED je u vlasništvu toka rezervacije
        // (reservation-service -> book/release); dozvoliti pružaocu usluge da ga ovde postavi
        // stvorilo bi fantomsku rezervaciju koja nikada ne može da se poništi kroz javni API.
        if (status != SlotStatus.AVAILABLE && status != SlotStatus.BLOCKED) {
            throw ApiException.badRequest("Termin može da se postavi samo na AVAILABLE ili BLOCKED");
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw ApiException.conflict("Termin je zauzet — prvo otkažite rezervaciju");
        }
        slot.setStatus(status);
        return SlotResponse.from(slot);
    }

    @Transactional
    public void delete(UUID slotId, UUID callerId, boolean admin) {
        var slot = getOrThrow(slotId);
        if (!admin && !slot.getProviderId().equals(callerId)) {
            throw ApiException.forbidden("Niste vlasnik ovog termina");
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw ApiException.conflict("Zauzet termin ne može da se obriše");
        }
        slotRepository.delete(slot);
    }

    // ---------- Interni API, poziva ga reservation-service ----------

    /**
     * Atomično označava termin kao BOOKED. Red se zaključava sa SELECT ... FOR UPDATE,
     * pa se paralelni pokušaji rezervacije serijalizuju, a gubitnik dobija 409.
     */
    @Transactional
    public SlotResponse book(UUID slotId) {
        var slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> ApiException.notFound("Termin " + slotId + " nije pronađen"));

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw ApiException.conflict("Termin je već zauzet");
        }
        if (slot.getStatus() == SlotStatus.BLOCKED) {
            throw ApiException.conflict("Termin je blokirao pružalac usluge");
        }
        if (!slot.getStartTime().isAfter(Instant.now())) {
            throw ApiException.conflict("Termin je u prošlosti");
        }

        slot.setStatus(SlotStatus.BOOKED);
        return SlotResponse.from(slot);
    }

    /** Vraća termin nazad na AVAILABLE (otkazivanje / kompenzaciona akcija). */
    @Transactional
    public SlotResponse release(UUID slotId) {
        var slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> ApiException.notFound("Termin " + slotId + " nije pronađen"));
        slot.setStatus(SlotStatus.AVAILABLE);
        return SlotResponse.from(slot);
    }

    private AvailabilitySlot getOrThrow(UUID id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Termin " + id + " nije pronađen"));
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
