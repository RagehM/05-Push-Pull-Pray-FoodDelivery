package com.team05.fooddelivery.checkout.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.LocalDate;
import java.util.Map;

/**
 * [S5-READ-DB] Response DTO for {@code GET /api/payments/user/{userId}/total}.
 *
 * Returns the total amount of COMPLETED payments for a user in the requested
 * date range, plus a per-method breakdown. The user's existence is verified
 * via Feign -> user-service, NOT via the local DB.
 */
@JsonDeserialize(builder = UserPaymentTotalDTO.Builder.class)
public class UserPaymentTotalDTO {

    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalPayments;
    private Double totalAmount;
    private Map<String, Double> methodBreakdown;

    private UserPaymentTotalDTO(Builder b) {
        this.userId = b.userId;
        this.startDate = b.startDate;
        this.endDate = b.endDate;
        this.totalPayments = b.totalPayments;
        this.totalAmount = b.totalAmount;
        this.methodBreakdown = b.methodBreakdown;
    }

    public Long getUserId() { return userId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Long getTotalPayments() { return totalPayments; }
    public Double getTotalAmount() { return totalAmount; }
    public Map<String, Double> getMethodBreakdown() { return methodBreakdown; }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Long userId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalPayments;
        private Double totalAmount;
        private Map<String, Double> methodBreakdown;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder totalPayments(Long totalPayments) { this.totalPayments = totalPayments; return this; }
        public Builder totalAmount(Double totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder methodBreakdown(Map<String, Double> methodBreakdown) { this.methodBreakdown = methodBreakdown; return this; }

        public UserPaymentTotalDTO build() { return new UserPaymentTotalDTO(this); }
    }
}
