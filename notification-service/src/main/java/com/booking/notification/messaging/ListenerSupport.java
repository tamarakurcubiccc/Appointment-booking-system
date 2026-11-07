package com.booking.notification.messaging;

import com.booking.common.exception.ApiException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

final class ListenerSupport {
    private ListenerSupport() {
    }

    static void dispatch(Runnable action) {
        try {
            action.run();
        } catch (ApiException ex) {
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }

    static String text(String value) {
        return value == null ? "" : value;
    }
}
