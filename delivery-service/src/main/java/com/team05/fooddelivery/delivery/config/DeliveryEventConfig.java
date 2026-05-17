package com.team05.fooddelivery.delivery.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class DeliveryEventConfig {
    private static final String DELIVERY_SAGA_LISTENER_DLQ = "delivery.saga-listener.dlq";
    private static final String DELIVERY_SAGA_LISTENER = "delivery.saga-listener";
    private static final String DELIVERY_SAGA_LISTENER_DLX = "delivery.saga-listener.dlx";

    private static final String DELIVERY_EVENTS_EXCHANGE = "delivery.events";
    private static final String ORDER_EVENTS_EXCHANGE = "order.events";

    private static final String ROUTING_ORDER_PLACED = "order.placed";
    private static final String ROUTING_ORDER_COMPLETED = "order.completed";
    private static final String ROUTING_ORDER_CANCELLED = "order.cancelled";

    // Exchanges
    @Bean
    public TopicExchange deliveryEventsExchange() {
        return new TopicExchange(
            DELIVERY_EVENTS_EXCHANGE,
            true,
            false
        );
    }

    // Reference to order.events (other services also declare this; it's fine)
    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(
            ORDER_EVENTS_EXCHANGE,
            true,
            false
        );
    }

    // Dead-letter exchange for consumer DLQ routing
    @Bean
    public TopicExchange deliverySagaDlx() {
        return new TopicExchange(
            DELIVERY_SAGA_LISTENER_DLX,
            true,
            false
        );
    }

    // Primary consumer queue with DLQ wiring
    @Bean
    public Queue deliverySagaListenerQueue() {
        return QueueBuilder.durable(DELIVERY_SAGA_LISTENER)
            .withArgument("x-dead-letter-exchange", DELIVERY_SAGA_LISTENER_DLX)
            .withArgument("x-dead-letter-routing-key", DELIVERY_SAGA_LISTENER_DLQ)
            .build();
    }

    // DLQ
    @Bean
    public Queue deliverySagaListenerDlq() {
        return QueueBuilder.durable(DELIVERY_SAGA_LISTENER_DLQ).build();
    }

    // Bind DLQ to the DLX
    @Bean
    public Binding dlqBinding(Queue deliverySagaListenerDlq, TopicExchange deliverySagaDlx) {
        return BindingBuilder.bind(deliverySagaListenerDlq)
                .to(deliverySagaDlx)
                .with(DELIVERY_SAGA_LISTENER_DLQ);
    }

    // Bindings: order.events -> delivery.saga-listener
    @Bean
    public Binding orderPlacedBinding(Queue deliverySagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(deliverySagaListenerQueue)
                .to(orderEventsExchange).with(ROUTING_ORDER_PLACED);
    }

    @Bean
    public Binding orderCompletedBinding(Queue deliverySagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(deliverySagaListenerQueue)
                .to(orderEventsExchange).with(ROUTING_ORDER_COMPLETED);
    }

    @Bean
    public Binding orderCancelledBinding(Queue deliverySagaListenerQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(deliverySagaListenerQueue)
                .to(orderEventsExchange).with(ROUTING_ORDER_CANCELLED);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                        MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }
}