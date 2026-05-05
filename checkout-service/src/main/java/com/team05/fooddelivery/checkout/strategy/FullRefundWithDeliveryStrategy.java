package com.team05.fooddelivery.checkout.strategy;

import com.team05.fooddelivery.checkout.dto.RefundRequest;
import com.team05.fooddelivery.checkout.dto.RefundResult;
import com.team05.fooddelivery.checkout.model.Payment;

import java.util.Map;

public class FullRefundWithDeliveryStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payment payment, RefundRequest request) {
//        Map<String, Object> transactionDetails = payment.getTransactionDetails();
//        Number deliveryFee = (Number) transactionDetails.get("deliveryFee");
//        Double refundedAmount = payment.getAmount() + deliveryFee.doubleValue();

        return new RefundResult(payment.getAmount(), "FULL_REFUND_WITH_DELIVERY");
    }
}
