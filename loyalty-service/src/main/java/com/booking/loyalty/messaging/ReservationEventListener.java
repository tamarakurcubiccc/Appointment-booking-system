package com.booking.loyalty.messaging;

import com.booking.common.event.ReservationCancelledEvent;
import com.booking.common.event.ReservationCreatedEvent;
import com.booking.common.exception.ApiException;
import com.booking.loyalty.config.RabbitConfig;
import com.booking.loyalty.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {
    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    private final LoyaltyService loyaltyService;

    public ReservationEventListener(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @RabbitListener(queues = RabbitConfig.RESERVATION_CREATED_QUEUE)
    public void onReservationCreated(ReservationCreatedEvent event) {
        log.info("Received reservation.created for reservation {}", event.reservationId());

        try {
            loyaltyService.registerAppointment(event.eventId(), event.clientId(), event.providerId());
        } catch (ApiException ex) {
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitConfig.RESERVATION_CANCELLED_QUEUE)
    public void onReservationCancelled(ReservationCancelledEvent event) {
        log.info("Received reservation.cancelled for reservation {}", event.reservationId());

        try {
            loyaltyService.registerCancellation(event.eventId(), event.clientId(),
                    event.providerId(), event.reservationId());
        } catch (ApiException ex) {
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }
}
