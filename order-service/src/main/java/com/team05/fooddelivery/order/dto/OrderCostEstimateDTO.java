package com.team05.fooddelivery.order.dto;

public record OrderCostEstimateDTO(
        Double estimatedFoodCost,
        Double deliveryFee,
        Double serviceFee,
        Double estimatedTotal,
        Double surgeMultiplier
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double estimatedFoodCost;
        private Double deliveryFee;
        private Double serviceFee;
        private Double estimatedTotal;
        private Double surgeMultiplier;

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

        public Builder estimatedTotal(Double estimatedTotal) {
            this.estimatedTotal = estimatedTotal;
            return this;
        }

        public Builder surgeMultiplier(Double surgeMultiplier) {
            this.surgeMultiplier = surgeMultiplier;
            return this;
        }

        public OrderCostEstimateDTO build() {
            return new OrderCostEstimateDTO(
                    estimatedFoodCost,
                    deliveryFee,
                    serviceFee,
                    estimatedTotal,
                    surgeMultiplier
            );
        }
    }
}