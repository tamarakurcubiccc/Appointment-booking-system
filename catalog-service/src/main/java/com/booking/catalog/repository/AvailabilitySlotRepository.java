package com.booking.catalog.repository;

import com.booking.catalog.entity.AvailabilitySlot;
import com.booking.catalog.entity.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {
    @Query("""
            select s from AvailabilitySlot s
            where s.service.id = :serviceId
              and s.startTime >= :from
              and s.startTime < :to
            order by s.startTime
            """)
    List<AvailabilitySlot> search(@Param("serviceId") UUID serviceId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("""
            select s from AvailabilitySlot s
            where s.service.id = :serviceId
              and s.startTime >= :from
              and s.startTime < :to
              and s.status = :status
            order by s.startTime
            """)
    List<AvailabilitySlot> searchByStatus(@Param("serviceId") UUID serviceId,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to,
                                          @Param("status") SlotStatus status);

    boolean existsByServiceIdAndStartTime(UUID serviceId, Instant startTime);

    @Query("select s.startTime from AvailabilitySlot s where s.service.id = :serviceId and s.startTime between :from and :to")
    List<Instant> findStartTimes(@Param("serviceId") UUID serviceId,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AvailabilitySlot s where s.id = :id")
    Optional<AvailabilitySlot> findByIdForUpdate(@Param("id") UUID id);
}
