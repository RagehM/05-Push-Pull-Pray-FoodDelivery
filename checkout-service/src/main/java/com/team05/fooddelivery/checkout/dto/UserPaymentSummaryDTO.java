package com.team05.fooddelivery.checkout.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonDeserialize(builder = UserPaymentSummaryDTO.Builder.class)
public class UserPaymentSummaryDTO {

    private Long userId;
    private Long totalPayments;
    private Double totalAmount;
    private Map<String, Double> methodBreakdown;

    private UserPaymentSummaryDTO(Builder builder) {
        this.userId = builder.userId;
        this.totalPayments = builder.totalPayments;
        this.totalAmount = builder.totalAmount;
        this.methodBreakdown = builder.methodBreakdown;
    }

    public Long getUserId() { return userId; }
    public Long getTotalPayments() { return totalPayments; }
    public Double getTotalAmount() { return totalAmount; }
    public Map<String, Double> getMethodBreakdown() { return methodBreakdown; }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Long userId;
        private Long totalPayments;
        private Double totalAmount;
        private Map<String, Double> methodBreakdown;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder totalPayments(Long totalPayments) { this.totalPayments = totalPayments; return this; }
        public Builder totalAmount(Double totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder methodBreakdown(Map<String, Double> methodBreakdown) { this.methodBreakdown = methodBreakdown; return this; }

        public UserPaymentSummaryDTO build() {
            return new UserPaymentSummaryDTO(this);
        }
    }
}