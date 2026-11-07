package com.booking.notification.messaging;

import com.booking.common.exception.ApiException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/** Zajedničko ponašanje potrošača poruka. */
final class ListenerSupport {

    private ListenerSupport() {
    }

    /**
     * Razlikuje prolaznu od determinističke greške.
     *
     * <p>Prolazna greška (SMTP nedostupan, user-service se restartuje) se propagira kao i jest,
     * pa je RabbitMQ ponavlja po podešenom backoff-u. Deterministička greška - šablon traži
     * parametar koji događaj ne nosi, primalac ne postoji - ne može da se "izleči" ponavljanjem,
     * pa poruka ide direktno na dead-letter red i oslobađa potrošača.
     */
    static void dispatch(Runnable action) {
        try {
            action.run();
        } catch (ApiException ex) {
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }

    /** Šablon ne sme da dobije null vrednost parametra. */
    static String text(String value) {
        return value == null ? "" : value;
    }
}
