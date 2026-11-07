package com.booking.loyalty.config;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP topologija ovog servisa. Isti događaji o rezervacijama idu i u notification-service:
 * topic exchange ih dostavlja u oba reda, pa servisi ne znaju jedan za drugog.
 *
 * <p>Dead-letter exchange je sopstveni, a ne onaj koji koristi notification-service.
 * Deljenje jednog DLX-a znači da bi svaki DLQ skupljao i tuđe neuspehe, jer poruka pri
 * dead-letteringu zadržava originalni routing key.
 */
@Configuration
public class RabbitConfig {

    public static final String RESERVATION_CREATED_QUEUE = "loyalty.reservation-created";
    public static final String RESERVATION_CANCELLED_QUEUE = "loyalty.reservation-cancelled";
    public static final String DEAD_LETTER_QUEUE = "loyalty.dlq";

    private static final String DEAD_LETTER_EXCHANGE = "loyalty.events.dlx";

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(BookingEvents.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange loyaltyDeadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue loyaltyReservationCreatedQueue() {
        return consumerQueue(RESERVATION_CREATED_QUEUE);
    }

    @Bean
    public Queue loyaltyReservationCancelledQueue() {
        return consumerQueue(RESERVATION_CANCELLED_QUEUE);
    }

    @Bean
    public Queue loyaltyDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding loyaltyReservationCreatedBinding() {
        return BindingBuilder.bind(loyaltyReservationCreatedQueue()).to(bookingEventsExchange())
                .with(BookingEvents.RESERVATION_CREATED);
    }

    @Bean
    public Binding loyaltyReservationCancelledBinding() {
        return BindingBuilder.bind(loyaltyReservationCancelledQueue()).to(bookingEventsExchange())
                .with(BookingEvents.RESERVATION_CANCELLED);
    }

    @Bean
    public Binding loyaltyDeadLetterBinding() {
        return BindingBuilder.bind(loyaltyDeadLetterQueue()).to(loyaltyDeadLetterExchange()).with("#");
    }

    /**
     * Konverter koristi ObjectMapper koji je konfigurisao Spring Boot; podrazumevani
     * konstruktor pravi mapper bez JavaTimeModule-a i Instant polja bi pucala.
     */
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
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }
}
