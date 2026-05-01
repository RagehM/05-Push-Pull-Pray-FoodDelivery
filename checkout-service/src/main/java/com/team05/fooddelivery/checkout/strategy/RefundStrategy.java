package com.team05.fooddelivery.checkout.strategy;

import com.team05.fooddelivery.checkout.dto.RefundRequest;
import com.team05.fooddelivery.checkout.dto.RefundResult;
import com.team05.fooddelivery.checkout.model.Payment;

public interface RefundStrategy {
    RefundResult calculateRefund(Payment payment, RefundRequest request);
}
