package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.model.neo4j.OrderedFrom;
import com.team05.fooddelivery.order.model.neo4j.RestaurantNode;
import com.team05.fooddelivery.order.model.neo4j.UserNode;
import com.team05.fooddelivery.order.repository.neo4j.RestaurantNodeRepository;
import com.team05.fooddelivery.order.repository.neo4j.UserNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderInteractionGraphService {

    private final UserNodeRepository userNodeRepository;
    private final RestaurantNodeRepository restaurantNodeRepository;

    public OrderInteractionGraphService(UserNodeRepository userNodeRepository,
                                        RestaurantNodeRepository restaurantNodeRepository) {
        this.userNodeRepository = userNodeRepository;
        this.restaurantNodeRepository = restaurantNodeRepository;
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public boolean upsertOrderedFromEdge(Long orderId,
                                         Long userId, String userName,
                                         Long restaurantId, String restaurantName, String cuisineType) {
        if (userNodeRepository.isOrderRecordedInRelationship(userId, restaurantId, orderId)) {
            return true;
        }

        UserNode user = userNodeRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserNode u = new UserNode(userName);
                    u.setUserId(userId);
                    return u;
                });
        if (user.getName() == null) user.setName(userName);

        RestaurantNode restaurant = restaurantNodeRepository.findByRestaurantId(restaurantId)
                .orElseGet(() -> {
                    RestaurantNode r = new RestaurantNode(restaurantName, cuisineType);
                    r.setRestaurantId(restaurantId);
                    return r;
                });
        if (restaurant.getName() == null) restaurant.setName(restaurantName);
        if (restaurant.getCuisineType() == null) restaurant.setCuisineType(cuisineType);

        OrderedFrom edge = user.getOrderedFroms().stream()
                .filter(of -> of.getRestaurant() != null
                        && restaurantId.equals(of.getRestaurant().getRestaurantId()))
                .findFirst()
                .orElse(null);

        if (edge == null) {
            edge = new OrderedFrom(restaurant);
            edge.setOrderCount(1);
            edge.setLastOrderDate(LocalDateTime.now());
            edge.recordOrderId(orderId);
            user.getOrderedFroms().add(edge);
        } else {
            edge.setOrderCount(edge.getOrderCount() + 1);
            edge.setLastOrderDate(LocalDateTime.now());
            edge.recordOrderId(orderId);
        }

        userNodeRepository.save(user);
        return false;
    }
}