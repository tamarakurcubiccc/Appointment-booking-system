package com.booking.notification.entity;

public enum NotificationStatus {
    /** Zapisano, slanje još nije pokušano. */
    PENDING,
    SENT,
    /** Poslednji pokušaj slanja nije uspeo; razlog je u failureReason. */
    FAILED
}
