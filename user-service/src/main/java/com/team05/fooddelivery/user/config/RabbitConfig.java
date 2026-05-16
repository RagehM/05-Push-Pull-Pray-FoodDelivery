package com.team05.fooddelivery.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String USER_EXCHANGE = "user.events";
    public static final String USER_SAGA_LISTENER_QUEUE = "user.order.saga-listener";
    public static final String USER_SAGA_LISTENER_DLQ = "user.order.saga-listener.dlq";
    public static final String USER_SAGA_LISTENER_DLX = "user.order.saga-listener.dlx";


    @Bean
    TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE,true,false);
    }

    @Bean
    TopicExchange userSagaDeadLetterExchange() {
        return new TopicExchange(USER_SAGA_LISTENER_DLX, true, false);
    }

    @Bean
    Queue userSageListenerQueue() {
        return  QueueBuilder
                .durable(USER_SAGA_LISTENER_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        USER_SAGA_LISTENER_DLX)
                .withArgument(
                        "x-dead-letter-routing-key",
                        USER_SAGA_LISTENER_DLQ)
                .build();

    }

    @Bean
    Queue userSageListenerDeadLetterQueue() {
        return QueueBuilder.durable(USER_SAGA_LISTENER_DLQ).build();
    }



    @Bean
    Binding userRegisteredBinding(@Qualifier("userSageListenerQueue") Queue q,@Qualifier("userExchange") TopicExchange exchange) {
        return BindingBuilder.bind(q).to(exchange).with("user.registered");
    }

    @Bean
    Binding userDeactivatedBinding(@Qualifier("userSageListenerQueue") Queue q,@Qualifier("userExchange") TopicExchange exchange) {
        return BindingBuilder.bind(q).to(exchange).with("user.deactivated");
    }

    @Bean
    Binding userSagaListenerDeadLetterBinding(@Qualifier("userSageListenerDeadLetterQueue") Queue userSageListenerDeadLetterQueue,
                                                 @Qualifier("userSagaDeadLetterExchange") TopicExchange userSagaDeadLetterExchange) {
        return BindingBuilder.bind(userSageListenerDeadLetterQueue)
                .to(userSagaDeadLetterExchange)
                .with(USER_SAGA_LISTENER_DLQ);
    }

    @Bean
    JacksonJsonMessageConverter jsonConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new JacksonJsonMessageConverter(String.valueOf(mapper));
    }

}
