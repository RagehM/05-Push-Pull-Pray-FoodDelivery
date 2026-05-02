package com.team05.fooddelivery.order.dto;

public class OrderCostEstimateDTO {
    private final Double estimatedFoodCost;
    private final Double deliveryFee;
    private final Double serviceFee;
    private final Double estimatedTotal;
    private final Double surgeMultiplier;

    private OrderCostEstimateDTO(Builder builder) {
        this.estimatedFoodCost = builder.estimatedFoodCost;
        this.deliveryFee = builder.deliveryFee;
        this.serviceFee = builder.serviceFee;
        this.estimatedTotal = builder.estimatedTotal != null ? builder.estimatedTotal : builder.estimatedFoodCost + builder.deliveryFee + builder.serviceFee;
        this.surgeMultiplier = builder.surgeMultiplier;
    }

    public Double getEstimatedFoodCost() { return estimatedFoodCost; }
    public Double getDeliveryFee() { return deliveryFee; }
    public Double getServiceFee() { return serviceFee; }
    public Double getEstimatedTotal() { return estimatedTotal; }
    public Double getSurgeMultiplier() { return surgeMultiplier; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double estimatedFoodCost;
        private Double deliveryFee;
        private Double serviceFee;
        private Double surgeMultiplier;
        private Double estimatedTotal = null;

        public Builder estimatedFoodCost(Double estimatedFoodCost) {
            this.estimatedFoodCost = estimatedFoodCost;
            return this;
        }

        public Builder deliveryFee(Double deliveryFee) {
            this.deliveryFee = deliveryFee;
            return this;
        }

        public Builder serviceFee(Double serviceFee) {
            this.serviceFee = serviceFee;
            return this;
        }

        public Builder surgeMultiplier(Double surgeMultiplier) {
            this.surgeMultiplier = surgeMultiplier;
            return this;
        }

        public Builder estimatedTotal(Double estimatedTotal) {
            this.estimatedTotal = estimatedTotal;
            return this;
        }

        public OrderCostEstimateDTO build() {
            if (estimatedFoodCost == null || estimatedFoodCost < 0)
                throw new IllegalStateException("estimatedFoodCost must be non-negative");
            if (deliveryFee == null || deliveryFee < 0)
                throw new IllegalStateException("deliveryFee must be non-negative");
            if (serviceFee == null || serviceFee < 0)
                throw new IllegalStateException("serviceFee must be non-negative");
            if (surgeMultiplier == null || surgeMultiplier < 1.0)
                throw new IllegalStateException("surgeMultiplier must be >= 1.0");

            return new OrderCostEstimateDTO(this);
        }
    }
}