package com.team05.fooddelivery.contracts.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "checkout-service", url = "${feign.checkout-service.url}")
public interface CheckoutServiceClient {

    @GetMapping("/api/payments/user/{userId}/total")
    BigDecimal getUserPaymentTotal(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );
}