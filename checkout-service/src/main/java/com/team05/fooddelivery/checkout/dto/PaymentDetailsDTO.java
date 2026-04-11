package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.PaymentMethod;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;

import java.util.List;
import java.util.Map;

public record PaymentDetailsDTO(
        Long paymentId,
        Long orderId,
        Long userId,
        Double originalAmount,
        PaymentMethod method,
        PaymentStatus status,
        Map<String, Object> transactionDetails,
        List<AppliedOfferDTO> appliedOffers,
        Double totalDiscount,
        Double finalAmount
) {}