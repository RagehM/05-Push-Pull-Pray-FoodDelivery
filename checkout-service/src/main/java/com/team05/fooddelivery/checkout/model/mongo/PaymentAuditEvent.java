package com.team05.fooddelivery.checkout.model.mongo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document for the payment_audit_trail collection.
 *
 * Schema (Section 7.1.6 of M2):
 *   id        : ObjectId (auto)
 *   paymentId : Long, references Payment in PG
 *   action    : CREATED | COMPLETED | FAILED | REFUNDED | REFUND_DENIED
 *               | ANALYTICS_VIEWED | OFFER_APPLIED | RETRY_ATTEMPTED | PAYMENT_DELETED
 *   timestamp : LocalDateTime
 *   method    : CREDIT_CARD | CASH_ON_DELIVERY | WALLET (required on payment-shaped actions)
 *   amount    : Double (required on payment-shaped actions)
 *   details   : Map<String,Object>
 *
 * S5-F11 reads documents whose {@code action} is COMPLETED or FAILED and whose
 * {@code method} and {@code amount} are populated, grouped by method.
 */
@Document("payment_audit_trail")
public class PaymentAuditEvent {

    @Id
    private String id;

    @Indexed
    private Long paymentId;

    @Indexed
    private String action;

    @Indexed
    private LocalDateTime timestamp;

    /**
     * Required (not-null) when {@code action} is one of CREATED, COMPLETED, FAILED,
     * REFUNDED, REFUND_DENIED, OFFER_APPLIED or RETRY_ATTEMPTED. Allowed null
     * for non-payment actions such as ANALYTICS_VIEWED.
     */
    private String method;

    /** See note on {@link #method}. */
    private Double amount;

    private Map<String, Object> details = new HashMap<>();

    public PaymentAuditEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentAuditEvent(Long paymentId,
                             String action,
                             String method,
                             Double amount,
                             Map<String, Object> details) {
        this.paymentId = paymentId;
        this.action = action;
        this.method = method;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.details = details != null ? details : new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    /** Canonical action constants; written in UPPER_SNAKE_CASE per Section 7.1.6. */
    public static final class Actions {
        private Actions() {}
        public static final String CREATED          = "CREATED";
        public static final String COMPLETED        = "COMPLETED";
        public static final String FAILED           = "FAILED";
        public static final String REFUNDED         = "REFUNDED";
        public static final String REFUND_DENIED    = "REFUND_DENIED";
        public static final String ANALYTICS_VIEWED = "ANALYTICS_VIEWED";
        public static final String OFFER_APPLIED    = "OFFER_APPLIED";
        public static final String RETRY_ATTEMPTED  = "RETRY_ATTEMPTED";
        public static final String PAYMENT_DELETED  = "PAYMENT_DELETED";
    }
}
