package com.booking.reservation.dto;

import com.booking.reservation.entity.Reservation;
import com.booking.reservation.entity.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID clientId,
        UUID providerId,
        UUID serviceId,
        UUID slotId,
        String serviceName,
        Instant startTime,
        Instant endTime,
        ReservationStatus status,
        String note,
        UUID rewardId,
        Instant createdAt,
        Instant cancelledAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getClientId(), r.getProviderId(), r.getServiceId(),
                r.getSlotId(), r.getServiceName(), r.getStartTime(), r.getEndTime(), r.getStatus(),
                r.getNote(), r.getRewardId(), r.getCreatedAt(), r.getCancelledAt());
    }
}
