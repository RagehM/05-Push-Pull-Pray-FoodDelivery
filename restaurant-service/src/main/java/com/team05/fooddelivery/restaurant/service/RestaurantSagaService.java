package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.OrderPlacedEvent;
import com.team05.fooddelivery.restaurant.model.ProcessedSagaEvent;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.ProcessedSagaEventRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class RestaurantSagaService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantSagaService.class);

    private final RestaurantRepository restaurantRepository;
    private final ProcessedSagaEventRepository processedSagaEventRepository;

    public RestaurantSagaService(RestaurantRepository restaurantRepository,
                                 ProcessedSagaEventRepository processedSagaEventRepository) {
        this.restaurantRepository = restaurantRepository;
        this.processedSagaEventRepository = processedSagaEventRepository;
    }

    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent event) {
        String key = sagaKey("order.placed", event.orderId());
        if (processedSagaEventRepository.existsById(key)) {
            log.info("Skipping duplicate order.placed for orderId={}", event.orderId());
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(event.restaurantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        int incoming = restaurant.getIncomingOrderCount() == null ? 0 : restaurant.getIncomingOrderCount();
        restaurant.setIncomingOrderCount(incoming + 1);
        restaurantRepository.save(restaurant);
        processedSagaEventRepository.save(new ProcessedSagaEvent(key, LocalDateTime.now()));

        log.info("Restaurant {} incomingOrderCount={} after order.placed",
                restaurant.getId(), restaurant.getIncomingOrderCount());
    }

    @Transactional
    public void handleOrderCompleted(OrderCompletedEvent event) {
        String key = sagaKey("order.completed", event.orderId());
        if (processedSagaEventRepository.existsById(key)) {
            log.info("Skipping duplicate order.completed for orderId={}", event.orderId());
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(event.restaurantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        long completed = restaurant.getSagaCompletedOrders() == null ? 0L : restaurant.getSagaCompletedOrders();
        double revenue = restaurant.getSagaCompletedRevenue() == null ? 0.0 : restaurant.getSagaCompletedRevenue();
        double amount = event.totalAmount() == null ? 0.0 : event.totalAmount().doubleValue();

        restaurant.setSagaCompletedOrders(completed + 1);
        restaurant.setSagaCompletedRevenue(revenue + amount);

        int incoming = restaurant.getIncomingOrderCount() == null ? 0 : restaurant.getIncomingOrderCount();
        if (incoming > 0) {
            restaurant.setIncomingOrderCount(incoming - 1);
        }

        restaurantRepository.save(restaurant);
        processedSagaEventRepository.save(new ProcessedSagaEvent(key, LocalDateTime.now(), amount));

        log.info("Restaurant {} saga stats updated after order.completed", restaurant.getId());
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        String cancelledKey = sagaKey("order.cancelled", event.orderId());
        if (processedSagaEventRepository.existsById(cancelledKey)) {
            log.info("Skipping duplicate order.cancelled for orderId={}", event.orderId());
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(event.restaurantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        String completedKey = sagaKey("order.completed", event.orderId());
        if (processedSagaEventRepository.existsById(completedKey)) {
            ProcessedSagaEvent completedRecord = processedSagaEventRepository.findById(completedKey).orElseThrow();
            double amount = completedRecord.getContextAmount() == null ? 0.0 : completedRecord.getContextAmount();

            long completed = restaurant.getSagaCompletedOrders() == null ? 0L : restaurant.getSagaCompletedOrders();
            double revenue = restaurant.getSagaCompletedRevenue() == null ? 0.0 : restaurant.getSagaCompletedRevenue();

            restaurant.setSagaCompletedOrders(Math.max(0, completed - 1));
            restaurant.setSagaCompletedRevenue(Math.max(0.0, revenue - amount));
            processedSagaEventRepository.deleteById(completedKey);
        } else {
            String placedKey = sagaKey("order.placed", event.orderId());
            if (processedSagaEventRepository.existsById(placedKey)) {
                int incoming = restaurant.getIncomingOrderCount() == null ? 0 : restaurant.getIncomingOrderCount();
                restaurant.setIncomingOrderCount(Math.max(0, incoming - 1));
                processedSagaEventRepository.deleteById(placedKey);
            }
        }

        restaurantRepository.save(restaurant);
        processedSagaEventRepository.save(new ProcessedSagaEvent(cancelledKey, LocalDateTime.now()));

        log.info("Restaurant {} saga stats reversed after order.cancelled", restaurant.getId());
    }

    private static String sagaKey(String routingKey, Long orderId) {
        return routingKey + ":" + orderId;
    }
}
