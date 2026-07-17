package com.booking.reservation.repository;

import com.booking.reservation.entity.Reservation;
import com.booking.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Page<Reservation> findByClientIdOrderByStartTimeDesc(UUID clientId, Pageable pageable);

    Page<Reservation> findByProviderIdOrderByStartTimeDesc(UUID providerId, Pageable pageable);

    boolean existsBySlotIdAndStatus(UUID slotId, ReservationStatus status);

    List<Reservation> findBySlotIdIn(List<UUID> slotIds);
}
