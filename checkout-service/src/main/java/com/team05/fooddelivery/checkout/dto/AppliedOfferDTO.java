package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.OfferDiscountType;
import java.time.LocalDateTime;

public record AppliedOfferDTO(
        String offerCode,
        OfferDiscountType discountType,
        Double discountApplied,
        LocalDateTime appliedAt
) {}