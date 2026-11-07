package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import com.booking.notification.entity.NotificationTemplate;
import com.booking.notification.mail.EmailSender;
import com.booking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationTemplateService templates;
    @Mock
    private NotificationStore store;
    @Mock
    private EmailSender emailSender;

    private NotificationService service;

    private UUID eventId;
    private UUID notificationId;
    private Recipient recipient;

    @BeforeEach
    void setUp() {
        // Renderer je prava implementacija - test proverava i tekst koji korisnik dobije.
        service = new NotificationService(repository, templates, new TemplateRenderer(), store, emailSender);

        eventId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        recipient = new Recipient(UUID.randomUUID(), "marko@example.com", "Marko Marković");
    }

    private NotificationTemplate template() {
        return new NotificationTemplate("RESERVATION_CONFIRMED_CLIENT", "Potvrda",
                "Potvrda - {{nazivUsluge}}", "Zdravo {{imeKlijenta}}, termin je {{vremeTermina}}.");
    }

    private Map<String, String> parameters() {
        return Map.of("imeKlijenta", "Marko Marković",
                "nazivUsluge", "Šišanje",
                "vremeTermina", "01.08.2026. 10:00");
    }

    @Test
    void sendsRenderedNotificationAndMarksItSent() {
        when(templates.findActive("RESERVATION_CONFIRMED_CLIENT")).thenReturn(Optional.of(template()));
        when(store.prepare(eq(eventId), anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.of(notificationId));

        service.dispatch(eventId, "RESERVATION_CONFIRMED_CLIENT", recipient, parameters());

        verify(emailSender).send("marko@example.com", "Potvrda - Šišanje",
                "Zdravo Marko Marković, termin je 01.08.2026. 10:00.");
        verify(store).markSent(notificationId);
    }

    @Test
    void skipsDispatchWhenTheTemplateIsInactive() {
        when(templates.findActive("RESERVATION_CONFIRMED_CLIENT")).thenReturn(Optional.empty());

        service.dispatch(eventId, "RESERVATION_CONFIRMED_CLIENT", recipient, parameters());

        verifyNoInteractions(store, emailSender);
    }

    @Test
    void ignoresADuplicateDeliveryOfTheSameEvent() {
        when(templates.findActive("RESERVATION_CONFIRMED_CLIENT")).thenReturn(Optional.of(template()));
        // Prazno = obaveštenje za ovaj događaj je već poslato.
        when(store.prepare(eq(eventId), anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        service.dispatch(eventId, "RESERVATION_CONFIRMED_CLIENT", recipient, parameters());

        verifyNoInteractions(emailSender);
        verify(store, never()).markSent(any());
    }

    @Test
    void marksTheNotificationFailedAndRethrowsWhenSmtpIsDown() {
        when(templates.findActive("RESERVATION_CONFIRMED_CLIENT")).thenReturn(Optional.of(template()));
        when(store.prepare(eq(eventId), anyString(), any(), anyString(), anyString()))
                .thenReturn(Optional.of(notificationId));
        doThrow(new MailSendException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());

        // Greška se propagira - potrošač poruke je vraća RabbitMQ-u na ponovni pokušaj.
        assertThatThrownBy(() -> service.dispatch(eventId, "RESERVATION_CONFIRMED_CLIENT", recipient, parameters()))
                .isInstanceOf(MailSendException.class);

        verify(store).markFailed(eq(notificationId), anyString());
        verify(store, never()).markSent(any());
    }

    @Test
    void failsBeforeWritingAnythingWhenAParameterIsMissing() {
        when(templates.findActive("RESERVATION_CONFIRMED_CLIENT")).thenReturn(Optional.of(template()));

        assertThatThrownBy(() -> service.dispatch(eventId, "RESERVATION_CONFIRMED_CLIENT", recipient,
                Map.of("imeKlijenta", "Marko Marković")))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(store, emailSender);
    }

    @Test
    void skipsRecipientWithoutAnEmailAddress() {
        when(templates.findActive("RESERVATION_CONFIRMED_CLIENT")).thenReturn(Optional.of(template()));

        service.dispatch(eventId, "RESERVATION_CONFIRMED_CLIENT",
                new Recipient(UUID.randomUUID(), null, "Bez e-maila"), parameters());

        verifyNoInteractions(store, emailSender);
    }
}
