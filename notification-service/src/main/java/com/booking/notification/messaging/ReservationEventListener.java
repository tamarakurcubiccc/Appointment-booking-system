package com.booking.notification.messaging;

import com.booking.common.event.ReservationCancelledEvent;
import com.booking.common.event.ReservationCreatedEvent;
import com.booking.notification.client.UserClient;
import com.booking.notification.config.RabbitConfig;
import com.booking.notification.service.MessageTimeFormatter;
import com.booking.notification.service.NotificationService;
import com.booking.notification.service.Recipient;
import com.booking.notification.service.TemplateCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Obaveštenja o rezervacijama. Obaveštavaju se obe strane - i klijent i pružalac usluge,
 * svaki svojim šablonom.
 *
 * <p>Kontakt podaci se dohvataju iz user-service u trenutku obrade poruke. Neuspeh tog
 * poziva je prolazna greška, pa RabbitMQ poruku ponavlja i nijedno obaveštenje se ne gubi
 * zato što je user-service bio nedostupan.
 */
@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    private final NotificationService notifications;
    private final UserClient userClient;
    private final MessageTimeFormatter timeFormatter;

    public ReservationEventListener(NotificationService notifications,
                                    UserClient userClient,
                                    MessageTimeFormatter timeFormatter) {
        this.notifications = notifications;
        this.userClient = userClient;
        this.timeFormatter = timeFormatter;
    }

    @RabbitListener(queues = RabbitConfig.RESERVATION_CREATED_QUEUE)
    public void onReservationCreated(ReservationCreatedEvent event) {
        log.info("Received reservation.created for reservation {}", event.reservationId());

        var client = Recipient.from(userClient.getUser(event.clientId()));
        var provider = Recipient.from(userClient.getUser(event.providerId()));
        String time = timeFormatter.format(event.startTime());

        ListenerSupport.dispatch(() -> {
            notifications.dispatch(event.eventId(), TemplateCodes.RESERVATION_CONFIRMED_CLIENT, client,
                    Map.of(
                            "imeKlijenta", ListenerSupport.text(client.fullName()),
                            "nazivUsluge", ListenerSupport.text(event.serviceName()),
                            "vremeTermina", time,
                            "imePruzaocaUsluge", ListenerSupport.text(provider.fullName())));

            notifications.dispatch(event.eventId(), TemplateCodes.RESERVATION_BOOKED_PROVIDER, provider,
                    Map.of(
                            "imePruzaocaUsluge", ListenerSupport.text(provider.fullName()),
                            "imeKlijenta", ListenerSupport.text(client.fullName()),
                            "nazivUsluge", ListenerSupport.text(event.serviceName()),
                            "vremeTermina", time));
        });
    }

    @RabbitListener(queues = RabbitConfig.RESERVATION_CANCELLED_QUEUE)
    public void onReservationCancelled(ReservationCancelledEvent event) {
        log.info("Received reservation.cancelled for reservation {}", event.reservationId());

        var client = Recipient.from(userClient.getUser(event.clientId()));
        var provider = Recipient.from(userClient.getUser(event.providerId()));
        String time = timeFormatter.format(event.startTime());

        // Ko je otkazao piše u obaveštenju - druga strana treba da zna da nije njena greška.
        String cancelledBy = event.clientId().equals(event.cancelledBy())
                ? ListenerSupport.text(client.fullName())
                : ListenerSupport.text(provider.fullName());

        ListenerSupport.dispatch(() -> {
            notifications.dispatch(event.eventId(), TemplateCodes.RESERVATION_CANCELLED_CLIENT, client,
                    Map.of(
                            "imeKlijenta", ListenerSupport.text(client.fullName()),
                            "nazivUsluge", ListenerSupport.text(event.serviceName()),
                            "vremeTermina", time,
                            "otkazao", cancelledBy));

            notifications.dispatch(event.eventId(), TemplateCodes.RESERVATION_CANCELLED_PROVIDER, provider,
                    Map.of(
                            "imePruzaocaUsluge", ListenerSupport.text(provider.fullName()),
                            "imeKlijenta", ListenerSupport.text(client.fullName()),
                            "nazivUsluge", ListenerSupport.text(event.serviceName()),
                            "vremeTermina", time,
                            "otkazao", cancelledBy));
        });
    }
}
