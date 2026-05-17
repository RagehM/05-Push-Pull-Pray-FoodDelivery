package com.team05.fooddelivery.delivery.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryEventConfig {

    // Exchanges
    @Bean
    public TopicExchange deliveryEventsExchange() {
        return new TopicExchange("delivery.events");
    }

    // Reference to order.events (other services also declare this; it's fine)
    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange("order.events");
    }

    // Dead-letter exchange for consumer DLQ routing
    @Bean
    public TopicExchange deliverySagaDlx() {
        return new TopicExchange("delivery.saga-listener.dlx");
    }

    // Primary consumer queue with DLQ wiring
    @Bean
    public Queue deliverySagaListenerQueue() {
        return QueueBuilder.durable("delivery.saga-listener")
                .withArgument("x-dead-letter-exchange", "delivery.saga-listener.dlx")
                .withArgument("x-dead-letter-routing-key", "delivery.saga-listener.dlq")
                .build();
    }

    // DLQ
    @Bean
    public Queue deliverySagaListenerDlq() {
        return QueueBuilder.durable("delivery.saga-listener.dlq").build();
    }

    // Bind DLQ to the DLX
    @Bean
    public Binding dlqBinding(Queue deliverySagaListenerDlq, TopicExchange deliverySagaDlx) {
        return BindingBuilder.bind(deliverySagaListenerDlq)
                .to(deliverySagaDlx)
                .with("delivery.saga-listener.dlq");
    }

    // Bindings: order.events -> delivery.saga-listener
    @Bean
    public Binding orderPlacedBinding(Queue deliverySagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(deliverySagaListenerQueue)
                .to(orderEventsExchange).with("order.placed");
    }

    @Bean
    public Binding orderCompletedBinding(Queue deliverySagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(deliverySagaListenerQueue)
                .to(orderEventsExchange).with("order.completed");
    }

    @Bean
    public Binding orderCancelledBinding(Queue deliverySagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(deliverySagaListenerQueue)
                .to(orderEventsExchange).with("order.cancelled");
    }
}