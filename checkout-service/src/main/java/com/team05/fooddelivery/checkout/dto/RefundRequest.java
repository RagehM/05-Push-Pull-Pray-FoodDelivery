package com.team05.fooddelivery.checkout.dto;

public record RefundRequest(boolean refundDeliveryFee, String reason) {}