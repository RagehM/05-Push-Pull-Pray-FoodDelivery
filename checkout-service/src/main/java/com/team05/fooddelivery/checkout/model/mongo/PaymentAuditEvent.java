package com.team05.fooddelivery.checkout.model.mongo;

import com.team05.fooddelivery.checkout.enums.PaymentAction;
import com.team05.fooddelivery.checkout.enums.PaymentMethod;

import com.mongodb.lang.NonNull;
import com.team05.shared.model.mongo.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "payment_audit_trail")
public class PaymentAuditEvent implements MongoEvent {

    @Id
    private String id;

    @Indexed
    private Long paymentId;

    private String action;

    private LocalDateTime timestamp;

    private String method;

    private Double amount;

    private Map<String, Object> details;

    public PaymentAuditEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentAuditEvent(Long paymentId, String action, LocalDateTime timestamp, String method, Double amount, Map<String, Object> details) {
        if(paymentId == null){
            throw new IllegalArgumentException("Invalid paymentId: " + paymentId);
        }
        if (action == null || !PaymentAction.isValidAction(action)) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        if(method == null){
            throw new IllegalArgumentException("Invalid method: " + method);
        }
        if(timestamp == null){
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp);
        }
        this.action = action;
        this.paymentId = paymentId;
        this.action = action;
        this.timestamp = timestamp;
        this.method = method;
        this.amount = amount;
        this.details = details;
    }

    public String getId() { return id; }

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
}