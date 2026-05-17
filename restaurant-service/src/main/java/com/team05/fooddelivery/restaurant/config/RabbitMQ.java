package com.team05.fooddelivery.restaurant.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
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
@EnableRabbit
public class RabbitMQ {

    // ── Exchanges ─────────────────────────────────────────────────────────────
    public static final String RESTAURANT_EVENTS_EXCHANGE = "restaurant.events";
    public static final String ORDER_EVENTS_EXCHANGE      = "order.events";

    // ── Routing keys this service PUBLISHES ───────────────────────────────────
    public static final String RESTAURANT_STATUS_CHANGED_ROUTING_KEY = "restaurant.status-changed";
    public static final String RESTAURANT_RATED_ROUTING_KEY          = "restaurant.rated";

    // ── Routing keys this service CONSUMES ────────────────────────────────────
    public static final String ORDER_PLACED_ROUTING_KEY    = "order.placed";
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    // ── Consumer queue + DLQ ──────────────────────────────────────────────────
    public static final String RESTAURANT_SAGA_LISTENER_QUEUE = "restaurant.order.saga-listener";
    public static final String RESTAURANT_SAGA_LISTENER_DLQ   = "restaurant.order.saga-listener.dlq";
    public static final String RESTAURANT_SAGA_LISTENER_DLX   = "restaurant.order.saga-listener.dlx";

    // ── Exchange beans ────────────────────────────────────────────────────────

    @Bean
    TopicExchange restaurantEventsExchange() {
        return new TopicExchange(RESTAURANT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange restaurantSagaDeadLetterExchange() {
        return new TopicExchange(RESTAURANT_SAGA_LISTENER_DLX, true, false);
    }

    // ── Queue beans ───────────────────────────────────────────────────────────

    @Bean
    Queue restaurantSagaListenerQueue() {
        return new Queue(
                RESTAURANT_SAGA_LISTENER_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange",    RESTAURANT_SAGA_LISTENER_DLX,
                        "x-dead-letter-routing-key", RESTAURANT_SAGA_LISTENER_DLQ
                )
        );
    }

    @Bean
    Queue restaurantSagaListenerDeadLetterQueue() {
        return new Queue(RESTAURANT_SAGA_LISTENER_DLQ, true);
    }

    // ── Binding beans ─────────────────────────────────────────────────────────

    @Bean
    Binding orderPlacedBinding(
            @Qualifier("restaurantSagaListenerQueue") Queue restaurantSagaListenerQueue,
            @Qualifier("orderEventsExchange")         TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(restaurantSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    Binding orderCompletedBinding(
            @Qualifier("restaurantSagaListenerQueue") Queue restaurantSagaListenerQueue,
            @Qualifier("orderEventsExchange")         TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(restaurantSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding orderCancelledBinding(
            @Qualifier("restaurantSagaListenerQueue") Queue restaurantSagaListenerQueue,
            @Qualifier("orderEventsExchange")         TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(restaurantSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    Binding restaurantSagaDeadLetterBinding(
            @Qualifier("restaurantSagaListenerDeadLetterQueue") Queue restaurantSagaListenerDeadLetterQueue,
            @Qualifier("restaurantSagaDeadLetterExchange")      TopicExchange restaurantSagaDeadLetterExchange) {
        return BindingBuilder.bind(restaurantSagaListenerDeadLetterQueue)
                .to(restaurantSagaDeadLetterExchange)
                .with(RESTAURANT_SAGA_LISTENER_DLQ);
    }

    // ── Infrastructure beans ──────────────────────────────────────────────────

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }
}