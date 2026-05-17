package com.team05.fooddelivery.user.messaging.publishers;

import com.team05.fooddelivery.contracts.events.UserDeactivatedEvent;
import com.team05.fooddelivery.contracts.events.UserRegisteredEvent;
import com.team05.fooddelivery.user.config.RabbitConfig;
import com.team05.fooddelivery.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRegisteredUser(User user) {
        var event = new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getRole().toString()
        );
        publish(RabbitConfig.USER_REGISTERED_ROUTING_KEY, event, user.getId());
    }

    public void publishDeactivatedUser(User user) {
        var event = new UserDeactivatedEvent(user.getId());
        publish(RabbitConfig.USER_DEACTIVATED_ROUTING_KEY, event, user.getId());
    }

    private void publish(String routingKey, Object payload, Long userId) {
        try {
            if (userId != null) MDC.put("userId", userId.toString());
            MDC.put("routingKey", routingKey);
            rabbitTemplate.convertAndSend(
                    RabbitConfig.USER_EVENTS_EXCHANGE,
                    routingKey,
                    payload,
                    message -> {
                        String correlationId = MDC.get("correlationId");
                        if (correlationId != null && !correlationId.isBlank()) {
                            message.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
                        }
                        return message;
                    }
            );
            log.info("Published {} for userId={}", routingKey, userId);
        } finally {
            MDC.remove("routingKey");
            if (userId != null) MDC.remove("userId");
        }
    }
}
