package com.team05.fooddelivery.user.config;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    // Exchanges
    public static final String USER_EVENTS_EXCHANGE  = "user.events";
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";

    // Routing keys – published by this service
    public static final String USER_REGISTERED_ROUTING_KEY  = "user.registered";
    public static final String USER_DEACTIVATED_ROUTING_KEY = "user.deactivated";

    // Routing keys – consumed by this service
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    // Queue / DLQ / DLX names
    public static final String USER_SAGA_LISTENER_QUEUE = "user.order.saga-listener";
    public static final String USER_SAGA_LISTENER_DLQ   = "user.order.saga-listener.dlq";
    public static final String USER_SAGA_LISTENER_DLX   = "user.order.saga-listener.dlx";

    // ── Exchanges ──────────────────────────────────────────────────────────────

    @Bean
    TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange userSagaDeadLetterExchange() {
        return new TopicExchange(USER_SAGA_LISTENER_DLX, true, false);
    }

    // ── Queues ─────────────────────────────────────────────────────────────────

    @Bean
    Queue userSagaListenerQueue() {
        return QueueBuilder
                .durable(USER_SAGA_LISTENER_QUEUE)
                .withArgument("x-dead-letter-exchange",    USER_SAGA_LISTENER_DLX)
                .withArgument("x-dead-letter-routing-key", USER_SAGA_LISTENER_DLQ)
                .build();
    }

    @Bean
    Queue userSagaListenerDeadLetterQueue() {
        return QueueBuilder.durable(USER_SAGA_LISTENER_DLQ).build();
    }

    // ── Bindings ───────────────────────────────────────────────────────────────

    /** Route order.completed events from order.events into our saga-listener queue */
    @Bean
    Binding orderCompletedBinding(@Qualifier("userSagaListenerQueue") Queue q,
                                  @Qualifier("orderEventsExchange") TopicExchange exchange) {
        return BindingBuilder.bind(q).to(exchange).with(ORDER_COMPLETED_ROUTING_KEY);
    }

    /** Route order.cancelled events from order.events into our saga-listener queue */
    @Bean
    Binding orderCancelledBinding(@Qualifier("userSagaListenerQueue") Queue q,
                                  @Qualifier("orderEventsExchange") TopicExchange exchange) {
        return BindingBuilder.bind(q).to(exchange).with(ORDER_CANCELLED_ROUTING_KEY);
    }

    /** DLQ binding */
    @Bean
    Binding userSagaListenerDeadLetterBinding(
            @Qualifier("userSagaListenerDeadLetterQueue") Queue dlq,
            @Qualifier("userSagaDeadLetterExchange")      TopicExchange dlx) {
        return BindingBuilder.bind(dlq).to(dlx).with(USER_SAGA_LISTENER_DLQ);
    }

    // ── Infrastructure beans ───────────────────────────────────────────────────

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
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
