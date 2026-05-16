package com.team05.fooddelivery.user.messaging;

import com.team05.fooddelivery.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.team05.fooddelivery.user.config.RabbitConfig;

public class OrderListener {
    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEvent.class);
    private final UserService userService;

    public OrderListener(UserService userService) {
        this.userService = userService;
    }


// TODO: Check Discord for an answer to how will the user service consume order events XD

//    @RabbitListener(queues = "order.completed")
//    public void handleCompletedOrder(OrderCreatedEvent event) {
//        try {
//            userService.
//            log.info("Reserved stock for order {} (async path)", event.orderId());
//        } catch (Exception e) {
//            log.warn("Failed to process order.created for order {}", event.orderId(), e);
//            throw e;   // trigger NACK → DLX
//        }
//    }
}
