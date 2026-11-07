package com.booking.reservation.messaging;

import com.booking.common.event.BookingEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Objavljuje događaj tek NAKON commit-a tekuće transakcije, isto kao što se i termin
     * oslobađa iz afterCommit-a: otkazivanje koje se vratilo nazad ne sme da pošalje
     * e-mail "termin je otkazan" za rezervaciju koja je i dalje potvrđena.
     */
    public void publish(String routingKey, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(routingKey, event);
                }
            });
        } else {
            send(routingKey, event);
        }
    }

    /**
     * Greška pri objavljivanju se loguje, a ne prosleđuje: rezervacija je već sačuvana i
     * termin je zauzet, pa pad zbog nedostupnog brokera ne bi doneo ništa dobro - klijent
     * bi dobio grešku za zakazivanje koje je uspelo. Cena je dostava "najviše jednom".
     */
    private void send(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(BookingEvents.EXCHANGE, routingKey, event);
            log.info("Published {} to {}", routingKey, BookingEvents.EXCHANGE);
        } catch (RuntimeException ex) {
            log.error("Failed to publish {} - the notification will not be sent", routingKey, ex);
        }
    }
}
