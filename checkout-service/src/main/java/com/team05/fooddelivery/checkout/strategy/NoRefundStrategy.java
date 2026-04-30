package com.team05.fooddelivery.checkout.strategy;

import com.team05.fooddelivery.checkout.dto.RefundRequest;
import com.team05.fooddelivery.checkout.dto.RefundResult;
import com.team05.fooddelivery.checkout.model.Payment;

public class NoRefundStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payment payment, RefundRequest request) {
        return new RefundResult(0.0, "refund window expired");
    }
}
