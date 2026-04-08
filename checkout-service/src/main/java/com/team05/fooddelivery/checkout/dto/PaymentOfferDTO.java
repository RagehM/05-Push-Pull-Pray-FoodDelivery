package com.team05.fooddelivery.checkout.dto;

import java.time.LocalDateTime;

public record PaymentOfferDTO(Double discountApplied, Long paymentId, Long offerId, LocalDateTime appliedAt) {}