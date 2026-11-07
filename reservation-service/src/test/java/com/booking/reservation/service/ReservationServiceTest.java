package com.booking.reservation.service;

import com.booking.common.event.BookingEvents;
import com.booking.common.event.ReservationCancelledEvent;
import com.booking.common.event.ReservationCreatedEvent;
import com.booking.common.exception.ApiException;
import com.booking.reservation.client.CatalogClient;
import com.booking.reservation.client.CatalogPublicClient;
import com.booking.reservation.client.LoyaltyClient;
import com.booking.reservation.client.LoyaltyRewardDto;
import com.booking.reservation.client.ServiceOfferingDto;
import com.booking.reservation.client.SlotDto;
import com.booking.reservation.client.UserClient;
import com.booking.reservation.client.RedeemRewardRequest;
import com.booking.reservation.client.UserDto;
import com.booking.reservation.dto.CreateReservationRequest;
import com.booking.reservation.entity.Reservation;
import com.booking.reservation.entity.ReservationStatus;
import com.booking.reservation.messaging.DomainEventPublisher;
import com.booking.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private ReservationRepository repository;
    @Mock
    private CatalogClient catalogClient;
    @Mock
    private CatalogPublicClient catalogPublicClient;
    @Mock
    private UserClient userClient;
    @Mock
    private LoyaltyClient loyaltyClient;
    @Mock
    private DomainEventPublisher events;

    @InjectMocks
    private ReservationService service;

    private UUID clientId;
    private UUID providerId;
    private UUID serviceId;
    private UUID slotId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        slotId = UUID.randomUUID();
    }

    private SlotDto slot(String status, Instant start) {
        return new SlotDto(slotId, serviceId, providerId, start, start.plus(30, ChronoUnit.MINUTES), status);
    }

    private ServiceOfferingDto offering(boolean active) {
        return new ServiceOfferingDto(serviceId, providerId, "Šišanje", 30, new BigDecimal("1500.00"), active);
    }

    @Test
    void booksAnAvailableSlot() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.book(clientId, new CreateReservationRequest(serviceId, slotId, "First visit", null));

        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.providerId()).isEqualTo(providerId);
        verify(catalogClient).bookSlot(slotId);
        verify(catalogClient, never()).releaseSlot(any());
    }

    @Test
    void rejectsSlotThatIsAlreadyBooked() {
        when(catalogClient.getSlot(slotId))
                .thenReturn(slot("BOOKED", Instant.now().plus(1, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(catalogClient, never()).bookSlot(any());
    }

    @Test
    void rejectsSlotBelongingToAnotherService() {
        var otherService = UUID.randomUUID();
        when(catalogClient.getSlot(slotId))
                .thenReturn(slot("AVAILABLE", Instant.now().plus(1, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> service.book(clientId, new CreateReservationRequest(otherService, slotId, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(catalogClient, never()).bookSlot(any());
    }

    @Test
    void rejectsSlotInThePast() {
        when(catalogClient.getSlot(slotId))
                .thenReturn(slot("AVAILABLE", Instant.now().minus(1, ChronoUnit.HOURS)));

        assertThatThrownBy(() -> service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void releasesTheSlotWhenPersistingTheReservationFails() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, null)))
                .isInstanceOf(IllegalStateException.class);

        verify(catalogClient, times(1)).releaseSlot(slotId);
    }

    @Test
    void cancellationFreesTheSlot() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var reservation = new Reservation(clientId, providerId, serviceId, slotId, "Šišanje",
                start, start.plus(30, ChronoUnit.MINUTES), null);
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(reservation));

        var response = service.cancel(id, clientId, false);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        verify(catalogClient).releaseSlot(slotId);
    }

    @Test
    void onlyTheProviderCanSeeTheClientsContactDetails() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var reservation = new Reservation(clientId, providerId, serviceId, slotId, "Šišanje",
                start, start.plus(30, ChronoUnit.MINUTES), null);
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(reservation));
        when(userClient.getUser(clientId))
                .thenReturn(new UserDto(clientId, "marko@example.com", "Marko Marković", "+381601112233", "CLIENT"));

        var details = service.clientDetails(id, providerId, false);
        assertThat(details.email()).isEqualTo("marko@example.com");

        assertThatThrownBy(() -> service.clientDetails(id, clientId, false))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void aStrangerCannotCancelSomeoneElsesReservation() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var reservation = new Reservation(clientId, providerId, serviceId, slotId, "Šišanje",
                start, start.plus(30, ChronoUnit.MINUTES), null);
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(id, UUID.randomUUID(), false))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(catalogClient, never()).releaseSlot(any());
    }

    @Test
    void bookingPublishesTheConfirmationEvent() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, null));

        var event = ArgumentCaptor.forClass(ReservationCreatedEvent.class);
        verify(events).publish(org.mockito.ArgumentMatchers.eq(BookingEvents.RESERVATION_CREATED), event.capture());
        assertThat(event.getValue().clientId()).isEqualTo(clientId);
        assertThat(event.getValue().providerId()).isEqualTo(providerId);
        assertThat(event.getValue().startTime()).isEqualTo(start);
    }

    @Test
    void aFailedBookingPublishesNothing() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, null)))
                .isInstanceOf(IllegalStateException.class);

        verify(events, never()).publish(any(), any());
    }

    @Test
    void cancellationPublishesWhoCancelled() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        var reservation = new Reservation(clientId, providerId, serviceId, slotId, "Šišanje",
                start, start.plus(30, ChronoUnit.MINUTES), null);
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(reservation));

        service.cancel(id, providerId, false);

        var event = ArgumentCaptor.forClass(ReservationCancelledEvent.class);
        verify(events).publish(org.mockito.ArgumentMatchers.eq(BookingEvents.RESERVATION_CANCELLED), event.capture());
        assertThat(event.getValue().cancelledBy()).isEqualTo(providerId);
        assertThat(event.getValue().serviceName()).isEqualTo("Šišanje");
    }

    @Test
    void bookingWithoutARewardNeverCallsTheLoyaltyService() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, null));

        verifyNoInteractions(loyaltyClient);
    }

    @Test
    void bookingWithARewardRedeemsIt() {
        var rewardId = UUID.randomUUID();
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyClient.usableRewards(clientId, providerId)).thenReturn(List.of(
                new LoyaltyRewardDto(rewardId, clientId, providerId, "Besplatan termin", "AVAILABLE", true)));

        var response = service.book(clientId, new CreateReservationRequest(serviceId, slotId, null, rewardId));

        assertThat(response.rewardId()).isEqualTo(rewardId);
        verify(loyaltyClient).redeem(any(RedeemRewardRequest.class));
    }

    @Test
    void aRewardThatIsNotUsableIsRefusedBeforeTheSlotIsTaken() {
        var rewardId = UUID.randomUUID();
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(loyaltyClient.usableRewards(clientId, providerId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.book(clientId,
                new CreateReservationRequest(serviceId, slotId, null, rewardId)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(catalogClient, never()).bookSlot(any());
        verify(catalogClient, never()).releaseSlot(any());
    }

    @Test
    void aRewardTakenInTheMeantimeRollsTheWholeBookingBack() {
        var rewardId = UUID.randomUUID();
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        when(catalogClient.getSlot(slotId)).thenReturn(slot("AVAILABLE", start));
        when(repository.existsBySlotIdAndStatus(slotId, ReservationStatus.CONFIRMED)).thenReturn(false);
        when(catalogPublicClient.getService(serviceId)).thenReturn(offering(true));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyClient.usableRewards(clientId, providerId)).thenReturn(List.of(
                new LoyaltyRewardDto(rewardId, clientId, providerId, "Besplatan termin", "AVAILABLE", true)));
        when(loyaltyClient.redeem(any(RedeemRewardRequest.class)))
                .thenThrow(ApiException.conflict("Nagrada je već iskorišćena"));

        assertThatThrownBy(() -> service.book(clientId,
                new CreateReservationRequest(serviceId, slotId, null, rewardId)))
                .isInstanceOf(ApiException.class);

        verify(repository).delete(any(Reservation.class));
        verify(catalogClient).releaseSlot(slotId);
        verify(events, never()).publish(any(), any());
    }
}
