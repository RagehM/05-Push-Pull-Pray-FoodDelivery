package com.team05.fooddelivery.user.messaging;

import com.team05.fooddelivery.user.model.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.team05.fooddelivery.user.config.RabbitConfig;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserPublisher {

    public static final String ROUTING_KEY_USER_REGISTERED = "user.registered";
    public static final String ROUTING_KEY_USER_DEACTIVATED = "user.deactivated";


    private final RabbitTemplate rabbit;

    public UserPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publishRegisteredUser(User user) {
        var event = new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getRole()
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
