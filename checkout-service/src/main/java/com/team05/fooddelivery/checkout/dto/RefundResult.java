package com.team05.fooddelivery.checkout.dto;

public record RefundResult(
        Double amount,
        String reasonCode
) {
}
