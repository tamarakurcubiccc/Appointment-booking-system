package com.booking.user.messaging;

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
 * Izdavač poznaje samo exchange i routing key. Redove i vezivanja deklariše potrošač
 * (notification-service), pa user-service ne mora da zna ko ga sluša - ni da li ga neko sluša.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange bookingEventsExchange() {
        return new TopicExchange(BookingEvents.EXCHANGE, true, false);
    }

    /**
     * Konverter koristi ObjectMapper koji je konfigurisao Spring Boot. Podrazumevani
     * konstruktor pravi sopstveni mapper bez JavaTimeModule-a, pa bi Instant polja
     * u događajima bila serijalizovana kao broj i potrošač bi ih drugačije pročitao.
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
