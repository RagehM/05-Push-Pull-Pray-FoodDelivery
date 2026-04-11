package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.OfferDiscountType;

public record OfferUsageDTO(
        Long offerId,
        String code,
        OfferDiscountType discountType,
        Double discountValue,
        Integer timesUsed,
        Double totalDiscountGiven,
        Boolean active,
        Boolean expired
) {}