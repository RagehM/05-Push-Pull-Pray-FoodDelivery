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

    public static final String RESTAURANT_EVENTS_EXCHANGE = "restaurant.events";
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";

    public static final String RESTAURANT_STATUS_CHANGED_ROUTING_KEY = "restaurant.status-changed";
    public static final String RESTAURANT_RATED_ROUTING_KEY = "restaurant.rated";

    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    public static final String RESTAURANT_ORDER_SAGA_QUEUE = "restaurant.order.saga-listener";
    public static final String RESTAURANT_ORDER_SAGA_DLQ = "restaurant.order.saga-listener.dlq";
    public static final String RESTAURANT_ORDER_SAGA_DLX = "restaurant.order.saga-listener.dlx";

    @Bean
    TopicExchange restaurantEventsExchange() {
        return new TopicExchange(RESTAURANT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange restaurantOrderSagaDeadLetterExchange() {
        return new TopicExchange(RESTAURANT_ORDER_SAGA_DLX, true, false);
    }

    @Bean
    Queue restaurantOrderSagaListenerQueue() {
        return new Queue(
                RESTAURANT_ORDER_SAGA_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", RESTAURANT_ORDER_SAGA_DLX,
                        "x-dead-letter-routing-key", RESTAURANT_ORDER_SAGA_DLQ
                )
        );
    }

    @Bean
    Queue restaurantOrderSagaDeadLetterQueue() {
        return new Queue(RESTAURANT_ORDER_SAGA_DLQ, true);
    }

    @Bean
    Binding orderPlacedBinding(
            @Qualifier("restaurantOrderSagaListenerQueue") Queue restaurantOrderSagaListenerQueue,
            @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(restaurantOrderSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    Binding orderCompletedBinding(
            @Qualifier("restaurantOrderSagaListenerQueue") Queue restaurantOrderSagaListenerQueue,
            @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(restaurantOrderSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding orderCancelledBinding(
            @Qualifier("restaurantOrderSagaListenerQueue") Queue restaurantOrderSagaListenerQueue,
            @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(restaurantOrderSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    Binding restaurantOrderSagaDeadLetterBinding(
            @Qualifier("restaurantOrderSagaDeadLetterQueue") Queue restaurantOrderSagaDeadLetterQueue,
            @Qualifier("restaurantOrderSagaDeadLetterExchange") TopicExchange restaurantOrderSagaDeadLetterExchange) {
        return BindingBuilder.bind(restaurantOrderSagaDeadLetterQueue)
                .to(restaurantOrderSagaDeadLetterExchange)
                .with(RESTAURANT_ORDER_SAGA_DLQ);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        return template;
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
