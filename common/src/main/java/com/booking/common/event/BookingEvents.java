package com.booking.common.event;

public final class BookingEvents {
    public static final String EXCHANGE = "booking.events";

    public static final String DEAD_LETTER_EXCHANGE = "booking.events.dlx";

    public static final String USER_REGISTERED = "user.registered";
    public static final String PASSWORD_RESET_REQUESTED = "user.password-reset-requested";
    public static final String RESERVATION_CREATED = "reservation.created";
    public static final String RESERVATION_CANCELLED = "reservation.cancelled";

    private BookingEvents() {
    }
}
