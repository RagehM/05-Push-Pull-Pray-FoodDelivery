package com.team05.fooddelivery.checkout;

import com.team05.fooddelivery.contracts.feign.OrderServiceClient;
import com.team05.fooddelivery.contracts.feign.RestaurantServiceClient;
import com.team05.fooddelivery.contracts.feign.UserServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

// [S5-READ-DB] Register only the Feign clients checkout-service actually consumes.
// We deliberately do NOT use basePackages here: the contracts module also contains
// CheckoutServiceClient (used by OTHER services to call us) and DeliveryServiceClient,
// neither of which defines a "feign.checkout-service.url" / "feign.delivery-service.url"
// in this service's application.yml. Scanning by package would try to register them
// here and fail to resolve their @FeignClient(url=...) placeholders at startup.
@SpringBootApplication
@EnableCaching
@EnableFeignClients(clients = {
        UserServiceClient.class,
        OrderServiceClient.class,
        RestaurantServiceClient.class
})
public class CheckoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }

}
