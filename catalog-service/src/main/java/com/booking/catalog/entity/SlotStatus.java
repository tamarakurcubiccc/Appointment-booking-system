package com.booking.catalog.entity;

public enum SlotStatus {
    /** Slobodan i može da se rezerviše. */
    AVAILABLE,
    /** Rezervisan od strane reservation-service. */
    BOOKED,
    /** Ručno blokiran od strane pružaoca usluge (praznik, pauza...). */
    BLOCKED
}
