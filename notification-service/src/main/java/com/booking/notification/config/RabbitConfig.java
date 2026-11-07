package com.booking.notification.config;

import com.booking.common.event.BookingEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class RabbitConfig {
    public static final String USER_REGISTERED_QUEUE = "notification.user-registered";
    public static final String PASSWORD_RESET_QUEUE = "notification.password-reset";
    public static final String RESERVATION_CREATED_QUEUE = "notification.reservation-created";
    public static final String RESERVATION_CANCELLED_QUEUE = "notification.reservation-cancelled";
    public static final String DEAD_LETTER_QUEUE = "notification.dlq";

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(BookingEvents.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange bookingEventsDeadLetterExchange() {
        return new TopicExchange(BookingEvents.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return consumerQueue(USER_REGISTERED_QUEUE);
    }

    @Bean
    public Queue passwordResetQueue() {
        return consumerQueue(PASSWORD_RESET_QUEUE);
    }

    @Bean
    public Queue reservationCreatedQueue() {
        return consumerQueue(RESERVATION_CREATED_QUEUE);
    }

    @Bean
    public Queue reservationCancelledQueue() {
        return consumerQueue(RESERVATION_CANCELLED_QUEUE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder.bind(userRegisteredQueue()).to(bookingEventsExchange())
                .with(BookingEvents.USER_REGISTERED);
    }

    @Bean
    public Binding passwordResetBinding() {
        return BindingBuilder.bind(passwordResetQueue()).to(bookingEventsExchange())
                .with(BookingEvents.PASSWORD_RESET_REQUESTED);
    }

    @Bean
    public Binding reservationCreatedBinding() {
        return BindingBuilder.bind(reservationCreatedQueue()).to(bookingEventsExchange())
                .with(BookingEvents.RESERVATION_CREATED);
    }

    @Bean
    public Binding reservationCancelledBinding() {
        return BindingBuilder.bind(reservationCancelledQueue()).to(bookingEventsExchange())
                .with(BookingEvents.RESERVATION_CANCELLED);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(bookingEventsDeadLetterExchange()).with("#");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        var converter = new Jackson2JsonMessageConverter(objectMapper);

        var typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("java.util", "java.lang", "com.booking.common.event");
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    private static Queue consumerQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(BookingEvents.DEAD_LETTER_EXCHANGE)
                .build();
    }
}
