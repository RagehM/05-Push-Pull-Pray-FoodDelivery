package com.team05.fooddelivery.checkout.config;

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
    public static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";

    public static final String PAYMENT_INITIATED_ROUTING_KEY = "payment.initiated";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";

    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    public static final String PAYMENT_SAGA_LISTENER_QUEUE = "payment.saga-listener";
    public static final String PAYMENT_SAGA_LISTENER_DLQ = "payment.saga-listener.dlq";
    public static final String PAYMENT_SAGA_LISTENER_DLX = "payment.saga-listener.dlx";

    @Bean
    TopicExchange paymentEventsExchange() {
        return new TopicExchange(PAYMENT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange paymentSagaDeadLetterExchange() {
        return new TopicExchange(PAYMENT_SAGA_LISTENER_DLX, true, false);
    }

    @Bean
    Queue paymentSagaListenerQueue() {
        return new Queue(
                PAYMENT_SAGA_LISTENER_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", PAYMENT_SAGA_LISTENER_DLX,
                        "x-dead-letter-routing-key", PAYMENT_SAGA_LISTENER_DLQ
                )
        );
    }

    @Bean
    Queue paymentSagaListenerDeadLetterQueue() {
        return new Queue(PAYMENT_SAGA_LISTENER_DLQ, true);
    }

    @Bean
    Binding orderCompletedBinding(@Qualifier("paymentSagaListenerQueue") Queue paymentSagaListenerQueue,
                                  @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(paymentSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding orderCancelledBinding(@Qualifier("paymentSagaListenerQueue") Queue paymentSagaListenerQueue,
                                  @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(paymentSagaListenerQueue)
                .to(orderEventsExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    Binding paymentSagaListenerDeadLetterBinding(@Qualifier("paymentSagaListenerDeadLetterQueue") Queue paymentSagaListenerDeadLetterQueue,
                                                 @Qualifier("paymentSagaDeadLetterExchange") TopicExchange paymentSagaDeadLetterExchange) {
        return BindingBuilder.bind(paymentSagaListenerDeadLetterQueue)
                .to(paymentSagaDeadLetterExchange)
                .with(PAYMENT_SAGA_LISTENER_DLQ);
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
