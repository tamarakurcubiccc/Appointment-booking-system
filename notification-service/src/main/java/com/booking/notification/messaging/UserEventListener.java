package com.booking.notification.messaging;

import com.booking.common.event.PasswordResetRequestedEvent;
import com.booking.common.event.UserRegisteredEvent;
import com.booking.notification.config.NotificationProperties;
import com.booking.notification.config.RabbitConfig;
import com.booking.notification.service.NotificationService;
import com.booking.notification.service.Recipient;
import com.booking.notification.service.TemplateCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Obaveštenja koja prate životni ciklus naloga. Podaci o korisniku dolaze u samom
 * događaju - događaj je o tom korisniku, pa dodatni poziv ka user-service nije potreban.
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final NotificationService notifications;
    private final NotificationProperties properties;

    public UserEventListener(NotificationService notifications, NotificationProperties properties) {
        this.notifications = notifications;
        this.properties = properties;
    }

    @RabbitListener(queues = RabbitConfig.USER_REGISTERED_QUEUE)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered for user {}", event.userId());

        var recipient = new Recipient(event.userId(), event.email(), event.fullName());
        ListenerSupport.dispatch(() -> notifications.dispatch(
                event.eventId(),
                TemplateCodes.ACCOUNT_ACTIVATION,
                recipient,
                Map.of(
                        "imeKorisnika", ListenerSupport.text(event.fullName()),
                        "aktivacioniLink", link(properties.activationBaseUrl(), event.activationToken()))));
    }

    @RabbitListener(queues = RabbitConfig.PASSWORD_RESET_QUEUE)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Received user.password-reset-requested for user {}", event.userId());

        var recipient = new Recipient(event.userId(), event.email(), event.fullName());
        ListenerSupport.dispatch(() -> notifications.dispatch(
                event.eventId(),
                TemplateCodes.PASSWORD_RESET,
                recipient,
                Map.of(
                        "imeKorisnika", ListenerSupport.text(event.fullName()),
                        "linkZaPromenuLozinke", link(properties.passwordResetBaseUrl(), event.resetToken()))));
    }

    private static String link(String baseUrl, String token) {
        return token == null ? baseUrl : baseUrl + "?token=" + token;
    }
}
