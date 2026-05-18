package com.team05.fooddelivery.checkout.messaging.publishers;

import com.team05.fooddelivery.checkout.config.RabbitMQ;
import com.team05.fooddelivery.contracts.events.PaymentCompletedEvent;
import com.team05.fooddelivery.contracts.events.PaymentFailedEvent;
import com.team05.fooddelivery.contracts.events.PaymentInitiatedEvent;
import com.team05.fooddelivery.contracts.events.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        publish(RabbitMQ.PAYMENT_INITIATED_ROUTING_KEY, event, event.paymentId());
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publish(RabbitMQ.PAYMENT_COMPLETED_ROUTING_KEY, event, event.paymentId());
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        publish(RabbitMQ.PAYMENT_FAILED_ROUTING_KEY, event, event.paymentId());
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        publish(RabbitMQ.PAYMENT_REFUNDED_ROUTING_KEY, event, event.paymentId());
    }

    private void publish(String routingKey, Object payload, Long paymentId) {
        try {
            if (paymentId != null) {
                MDC.put("paymentId", paymentId.toString());
            }
            MDC.put("routingKey", routingKey);
            rabbitTemplate.convertAndSend(
                    RabbitMQ.PAYMENT_EVENTS_EXCHANGE,
                    routingKey,
                    payload,
                    message -> {
                        String correlationId = MDC.get("correlationId");
                        String jwtToken = MDC.get("jwtToken");
                        if (correlationId != null && !correlationId.isBlank()) {
                            message.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
                        }
                        if (jwtToken != null) {
                            message.getMessageProperties().setHeader("Authorization", "Bearer " + jwtToken);
                            // System.err.println("Publisher - Adding Authorization header with JWT token = Bearer " + jwtToken);
                        }
                        return message;
                    }
            );
            log.info("Published {} for {}={}", routingKey, "paymentId", paymentId);
        } finally {
            MDC.remove("routingKey");
            if (paymentId != null) {
                MDC.remove("paymentId");
            }
        }
    }
}

