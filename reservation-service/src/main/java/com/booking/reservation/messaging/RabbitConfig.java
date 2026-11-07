package com.booking.reservation.messaging;

import com.booking.common.event.BookingEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Izdavač poznaje samo exchange i routing key - redove i vezivanja deklariše potrošač.
 * Deklaracija exchange-a je idempotentna, pa to što je deklariše i user-service nije sukob.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(BookingEvents.EXCHANGE, true, false);
    }

    /**
     * Konverter koristi ObjectMapper koji je konfigurisao Spring Boot - podrazumevani
     * konstruktor pravi sopstveni mapper bez JavaTimeModule-a, pa bi vremena termina
     * u događaju bila zapisana kao broj, a potrošač ih čita kao ISO tekst.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
