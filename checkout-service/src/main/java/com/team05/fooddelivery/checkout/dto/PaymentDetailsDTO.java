package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.PaymentMethod;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;

import java.util.List;
import java.util.Map;

public class PaymentDetailsDTO {

    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Double originalAmount;
    private PaymentMethod method;
    private PaymentStatus status;
    private Map<String, Object> transactionDetails;
    private List<AppliedOfferDTO> appliedOffers;
    private Double totalDiscount;
    private Double finalAmount;

    private PaymentDetailsDTO(Builder builder) {
        this.paymentId = builder.paymentId;
        this.orderId = builder.orderId;
        this.userId = builder.userId;
        this.originalAmount = builder.originalAmount;
        this.method = builder.method;
        this.status = builder.status;
        this.transactionDetails = builder.transactionDetails;
        this.appliedOffers = builder.appliedOffers;
        this.totalDiscount = builder.totalDiscount;
        this.finalAmount = builder.finalAmount;
    }

    public Long getPaymentId() { return paymentId; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Double getOriginalAmount() { return originalAmount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public Map<String, Object> getTransactionDetails() { return transactionDetails; }
    public List<AppliedOfferDTO> getAppliedOffers() { return appliedOffers; }
    public Double getTotalDiscount() { return totalDiscount; }
    public Double getFinalAmount() { return finalAmount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long paymentId;
        private Long orderId;
        private Long userId;
        private Double originalAmount;
        private PaymentMethod method;
        private PaymentStatus status;
        private Map<String, Object> transactionDetails;
        private List<AppliedOfferDTO> appliedOffers;
        private Double totalDiscount;
        private Double finalAmount;

        public Builder paymentId(Long paymentId) { this.paymentId = paymentId; return this; }
        public Builder orderId(Long orderId) { this.orderId = orderId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder originalAmount(Double originalAmount) { this.originalAmount = originalAmount; return this; }
        public Builder method(PaymentMethod method) { this.method = method; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
        public Builder transactionDetails(Map<String, Object> transactionDetails) { this.transactionDetails = transactionDetails; return this; }
        public Builder appliedOffers(List<AppliedOfferDTO> appliedOffers) { this.appliedOffers = appliedOffers; return this; }
        public Builder totalDiscount(Double totalDiscount) { this.totalDiscount = totalDiscount; return this; }
        public Builder finalAmount(Double finalAmount) { this.finalAmount = finalAmount; return this; }

        public PaymentDetailsDTO build() {
            return new PaymentDetailsDTO(this);
        }
    }
}