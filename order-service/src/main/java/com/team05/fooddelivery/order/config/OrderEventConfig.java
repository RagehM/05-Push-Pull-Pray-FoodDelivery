package com.team05.fooddelivery.order.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderEventConfig {
    @Bean
    TopicExchange orderExchange() {
        return new TopicExchange(
            "order.events",
            true,
            false   
        );
    }

    @Bean
    TopicExchange orderSagaExchange() {
        return new TopicExchange(
            "order.saga-feedback.dlx",
            true,
            false   
        );
    }

    @Bean
    Queue sagaFeedbackDQL() {
        return QueueBuilder.
            durable("order.saga-feedback.dlq").
            build();
    }

    @Bean
    Queue sagaFeedbackQueue() {
        return QueueBuilder.
            durable("order.saga-feedback").
            withArgument("x-dead-letter-exchange", "order.saga-feedback.dlx").
            withArgument("x-dead-letter-routing-key", "order.saga-feedback.dlq").
            build();
    }

    @Bean
    TopicExchange deliveryExchange() {
        return new TopicExchange(
            "delivery.events",
            true,
            false   
        );
    }

    @Bean
    TopicExchange paymentExchange() {
        return new TopicExchange(
            "payment.events",
            true,
            false   
        );
    }

    @Bean
    Binding sagaFeedbackDLQBinding(Queue sagaFeedbackDQL, TopicExchange orderSagaExchange) {
        return BindingBuilder.
            bind(sagaFeedbackDQL).
            to(orderSagaExchange).
            with("order.saga-feedback.dlq");
    }

    @Bean
    Binding deliveryCreatedBinding(Queue sagaFeedbackQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.
            bind(sagaFeedbackQueue).
            to(deliveryExchange).
            with("delivery.created");
    }

    @Bean
    Binding paymentInitiatedBinding(Queue sagaFeedbackQueue, TopicExchange paymentExchange) {
        return BindingBuilder.
            bind(sagaFeedbackQueue).
            to(paymentExchange).
            with("payment.initiated");
    }

    @Bean
    Binding paymentCompleteBinding(Queue sagaFeedbackQueue, TopicExchange paymentExchange) {
        return BindingBuilder.
            bind(sagaFeedbackQueue).
            to(paymentExchange).
            with("payment.completed");
    }

    @Bean
    Binding paymentFailedBinding(Queue sagaFeedbackQueue, TopicExchange paymentExchange) {
        return BindingBuilder.
            bind(sagaFeedbackQueue).
            to(paymentExchange).
            with("payment.failed");
    }
    
    @Bean
    Binding paymentRefundedBinding(Queue sagaFeedbackQueue, TopicExchange paymentExchange) {
        return BindingBuilder.
            bind(sagaFeedbackQueue).
            to(paymentExchange).
            with("payment.refunded");
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
