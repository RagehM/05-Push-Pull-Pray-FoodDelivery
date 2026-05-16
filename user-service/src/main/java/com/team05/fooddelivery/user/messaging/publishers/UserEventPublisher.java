package com.team05.fooddelivery.user.messaging.publishers;

import com.team05.fooddelivery.contracts.events.UserDeactivatedEvent;
import com.team05.fooddelivery.contracts.events.UserRegisteredEvent;
import com.team05.fooddelivery.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.team05.fooddelivery.user.config.RabbitConfig;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    public static final String ROUTING_KEY_USER_REGISTERED = "user.registered";
    public static final String ROUTING_KEY_USER_DEACTIVATED = "user.deactivated";

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbit;

    public UserEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publishRegisteredUser(User user) {
        var event = new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getRole().toString()
        );
        rabbit.convertAndSend(
                RabbitConfig.USER_EXCHANGE,
                ROUTING_KEY_USER_REGISTERED,
                event
        );
    }

    public void publishDeactivatedUser(User user) {
        var event = new UserDeactivatedEvent(
                user.getId()
        );
        rabbit.convertAndSend(
                RabbitConfig.USER_EXCHANGE,
                ROUTING_KEY_USER_DEACTIVATED,
                event
        );
    }
}
