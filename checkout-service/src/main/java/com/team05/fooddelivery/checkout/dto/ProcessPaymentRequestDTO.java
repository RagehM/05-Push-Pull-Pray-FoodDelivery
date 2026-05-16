package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.PaymentMethod;

public record ProcessPaymentRequestDTO(
		Long orderId,
		Long userId,
		Double amount,
		PaymentMethod method,
		String cardLastFour
){}