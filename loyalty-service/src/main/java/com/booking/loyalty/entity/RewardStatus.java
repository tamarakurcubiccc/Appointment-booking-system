package com.booking.loyalty.entity;

public enum RewardStatus {
    /** Dodeljena i spremna za korišćenje. */
    AVAILABLE,
    /** Iskorišćena pri zakazivanju termina. */
    REDEEMED,
    /** Istekla pre nego što je iskorišćena. */
    EXPIRED
}
