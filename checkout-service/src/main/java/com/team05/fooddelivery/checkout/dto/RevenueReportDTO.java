package com.team05.fooddelivery.checkout.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = RevenueReportDTO.Builder.class)
public class RevenueReportDTO {

    private Double totalRevenue;
    private Integer totalTransactions;
    private Double averagePayment;
    private Double refundedAmount;
    private Integer refundCount;

    private RevenueReportDTO(Builder builder) {
        this.totalRevenue = builder.totalRevenue;
        this.totalTransactions = builder.totalTransactions;
        this.averagePayment = builder.averagePayment;
        this.refundedAmount = builder.refundedAmount;
        this.refundCount = builder.refundCount;
    }

    public Double getTotalRevenue() { return totalRevenue; }
    public Integer getTotalTransactions() { return totalTransactions; }
    public Double getAveragePayment() { return averagePayment; }
    public Double getRefundedAmount() { return refundedAmount; }
    public Integer getRefundCount() { return refundCount; }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Double totalRevenue;
        private Integer totalTransactions;
        private Double averagePayment;
        private Double refundedAmount;
        private Integer refundCount;

        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder totalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; return this; }
        public Builder averagePayment(Double averagePayment) { this.averagePayment = averagePayment; return this; }
        public Builder refundedAmount(Double refundedAmount) { this.refundedAmount = refundedAmount; return this; }
        public Builder refundCount(Integer refundCount) { this.refundCount = refundCount; return this; }

        public RevenueReportDTO build() {
            return new RevenueReportDTO(this);
        }
    }
}