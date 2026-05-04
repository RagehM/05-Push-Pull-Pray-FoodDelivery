package com.team05.shared.model.mongo;

import com.team05.shared.enums.PaymentAction;


import com.mongodb.lang.NonNull;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@Document(collection = "payment_audit_trail")
public class PaymentAuditEvent implements MongoEvent {

    @Id
    private String id;

    @Indexed
    private Long paymentId;

    @Indexed
    private String action;

    @Indexed
    private LocalDateTime timestamp;

    private String method;

    private Double amount;

    private Map<String, Object> details = new HashMap<>();

    public PaymentAuditEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentAuditEvent(Long paymentId, String action, String method, Double amount, Map<String, Object> details) {
        if(paymentId == null){
            throw new IllegalArgumentException("paymentId must not be null");
        }
        if (action == null || !PaymentAction.isValidAction(action)) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        this.paymentId = paymentId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.method = method;
        this.amount = amount;
        this.details = details;
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