package com.team05.fooddelivery.checkout.messaging.consumers;

import com.team05.fooddelivery.checkout.config.RabbitMQ;
import com.team05.fooddelivery.checkout.messaging.publishers.PaymentEventPublisher;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.service.PaymentService;
import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.PaymentInitiatedEvent;
import com.team05.fooddelivery.contracts.events.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RabbitListener(queues = RabbitMQ.PAYMENT_SAGA_LISTENER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final PaymentService paymentService;
    private final PaymentEventPublisher paymentEventPublisher;

    public OrderEventConsumer(PaymentService paymentService, PaymentEventPublisher paymentEventPublisher) {
        this.paymentService = paymentService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @RabbitHandler
    public void onOrderCompleted(
            OrderCompletedEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitMQ.ORDER_COMPLETED_ROUTING_KEY, correlationId, event.orderId());
        try {
            log.info("Consuming order.completed for orderId={}", event.orderId());

            Payment payment = paymentService.createPendingPayment(
                    event.orderId(), event.userId(), event.totalAmount());

            paymentEventPublisher.publishPaymentInitiated(new PaymentInitiatedEvent(
                    payment.getId(),
                    payment.getOrderId(),
                    BigDecimal.valueOf(payment.getAmount())
            ));

            log.info("Processed order.completed for orderId={} — created paymentId={}",
                    event.orderId(), payment.getId());
        } catch (Exception ex) {
            log.error("Failed to process order.completed for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
            throw ex; // let retry / DLQ handle it
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler
    public void onOrderCancelled(
            OrderCancelledEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitMQ.ORDER_CANCELLED_ROUTING_KEY, correlationId, event.orderId());
        try {
            log.info("Consuming order.cancelled for orderId={}", event.orderId());

            Optional<Payment> refundedOpt =
                    paymentService.refundPaymentForCancelledOrder(event.orderId());

            if (refundedOpt.isEmpty()) {
                log.info("No PENDING/COMPLETED payment found for orderId={} — nothing to refund",
                        event.orderId());
                return;
            }

            Payment refunded = refundedOpt.get();
            paymentEventPublisher.publishPaymentRefunded(new PaymentRefundedEvent(
                    refunded.getId(),
                    refunded.getOrderId(),
                    BigDecimal.valueOf(refunded.getAmount())
            ));

            log.info("Processed order.cancelled for orderId={} — refunded paymentId={}",
                    event.orderId(), refunded.getId());
        } catch (Exception ex) {
            log.error("Failed to process order.cancelled for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object unknownPayload) {
        log.warn("payment.saga-listener received an unknown message type: {}",
                unknownPayload == null ? "null" : unknownPayload.getClass().getName());
    }

    private void setupMdc(String routingKey, String correlationId, Long orderId) {
        if (routingKey != null)    MDC.put("routingKey",    routingKey);
        if (correlationId != null) MDC.put("correlationId", correlationId);
        if (orderId != null)       MDC.put("orderId",       orderId.toString());
    }

    private void clearMdc() {
        MDC.remove("routingKey");
        MDC.remove("correlationId");
        MDC.remove("orderId");
        MDC.remove("paymentId");
    }
}





