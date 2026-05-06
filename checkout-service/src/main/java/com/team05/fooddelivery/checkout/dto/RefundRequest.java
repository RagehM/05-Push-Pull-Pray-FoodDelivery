package com.team05.fooddelivery.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefundRequest(
        @JsonProperty("refundDeliveryFee") boolean refundDeliveryFee,
        @JsonProperty("reason") String reason
) {}
