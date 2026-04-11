package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.PaymentMethod;

public record ProcessPaymentRequestDTO(PaymentMethod method, String cardLastFour){}