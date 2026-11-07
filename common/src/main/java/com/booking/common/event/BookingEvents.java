package com.booking.common.event;

/**
 * Imena AMQP topologije i routing key-evi domenskih događaja.
 *
 * <p>Ugovor (ovaj paket) namerno živi u {@code common} modulu: i izdavač i potrošač
 * koriste iste rekorde, pa Jackson na strani potrošača razrešava {@code __TypeId__}
 * zaglavlje u istu klasu. Modul ne zavisi od Spring AMQP-a - sadrži samo ugovor,
 * dok svaki servis sam deklariše svoje redove i vezivanja.
 */
public final class BookingEvents {

    /** Topic exchange kroz koji prolaze svi domenski događaji. */
    public static final String EXCHANGE = "booking.events";

    /** Dead-letter exchange - prima poruke koje potrošač nije uspeo da obradi. */
    public static final String DEAD_LETTER_EXCHANGE = "booking.events.dlx";

    public static final String USER_REGISTERED = "user.registered";
    public static final String PASSWORD_RESET_REQUESTED = "user.password-reset-requested";
    public static final String RESERVATION_CREATED = "reservation.created";
    public static final String RESERVATION_CANCELLED = "reservation.cancelled";

    private BookingEvents() {
    }
}
