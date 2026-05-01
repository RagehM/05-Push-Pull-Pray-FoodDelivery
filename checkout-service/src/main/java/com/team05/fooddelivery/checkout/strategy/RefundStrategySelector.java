package com.team05.fooddelivery.checkout.strategy;

import com.team05.fooddelivery.checkout.dto.RefundRequest;
import com.team05.fooddelivery.checkout.dto.RefundResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RefundStrategySelector {

    public RefundStrategy select(RefundRequest refundRequest, LocalDateTime paymentCreatedAt) {
        boolean withinWindow = paymentCreatedAt.isAfter(LocalDateTime.now().minusHours(24));

        if(!withinWindow) {
            return new NoRefundStrategy();
        }
        if(refundRequest.refundDeliveryFee()) {
            return new FullRefundWithDeliveryStrategy();
        } else {
            return new FoodOnlyRefundStrategy();
        }
    }
}
