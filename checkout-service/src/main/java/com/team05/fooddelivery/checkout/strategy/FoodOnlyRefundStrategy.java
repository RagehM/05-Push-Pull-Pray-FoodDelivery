package com.team05.fooddelivery.checkout.strategy;

import com.team05.fooddelivery.checkout.dto.RefundRequest;
import com.team05.fooddelivery.checkout.dto.RefundResult;
import com.team05.fooddelivery.checkout.model.Payment;

public class FoodOnlyRefundStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payment payment, RefundRequest request) {
        Double refundedAmount = payment.getAmount() - request.deliveryFee();
        return new RefundResult(refundedAmount, "FULL_REFUND_WITHOUT_DELIVERY");
    }
}
