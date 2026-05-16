package com.team05.fooddelivery.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String USER_EXCHANGE = "user.events";


    @Bean
    TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE,true,false);
    }

    @Bean
    Queue userRegisteredQueue() {
        return QueueBuilder.durable("user.queue.registered")
                .build();
    }

    @Bean
    Queue userDeactivatedQueue() {
        return QueueBuilder.durable("user.queue.deactivated").build();
    }

    @Bean
    Binding userBinding(@Qualifier("userRegisteredQueue") Queue q, TopicExchange exchange) {
        return BindingBuilder.bind(q).to(exchange).with("user.registered");
    }

    @Bean
    Binding userDeactivatedBinding(@Qualifier("userDeactivatedQueue") Queue q, TopicExchange exchange) {
        return BindingBuilder.bind(q).to(exchange).with("user.deactivated");
    }

    @Bean
    JacksonJsonMessageConverter jsonConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JacksonJsonMessageConverter(String.valueOf(mapper));
    }

}
