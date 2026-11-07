package com.booking.user.messaging;

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
     * Objavljuje događaj tek NAKON što se tekuća transakcija uspešno komituje.
     *
     * <p>Objavljivanje pre commit-a znači da korisnik dobije e-mail "nalog je kreiran" i za
     * registraciju koja se na kraju vratila nazad. Zato se poruka šalje iz afterCommit-a.
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
     * Greška pri objavljivanju se loguje, a ne prosleđuje dalje: registracija koja je već
     * sačuvana ne sme da padne zato što broker nije dostupan. Posledica je dostava "najviše
     * jednom" - izgubljeno obaveštenje se ne nadoknađuje automatski. Garancija "najmanje
     * jednom" bi zahtevala outbox tabelu u istoj transakciji i zadatak koji je prazni.
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
