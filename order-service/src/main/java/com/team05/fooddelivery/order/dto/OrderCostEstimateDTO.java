package com.team05.fooddelivery.order.dto;

public record OrderCostEstimateDTO(
     Double estimatedFoodCost,
     Double deliveryFee,
     Double serviceFee,
     Double estimatedTotal,
     Double surgeMultiplier
){}
