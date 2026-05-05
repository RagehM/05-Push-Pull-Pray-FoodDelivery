package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.adapter.ObjectArrayToOrderAnalyticsDashboardDTOAdapter;
import com.team05.fooddelivery.order.adapter.orderArrayToOrderAnalyticsDTOAdapter;
import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import com.team05.fooddelivery.order.dto.OrderAnalyticsDashboardDTO;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.dto.OrderCostEstimateDTO;
import com.team05.fooddelivery.order.dto.OrderEstimateRequest;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.dto.OrderDetailsDTO;
import com.team05.fooddelivery.order.dto.OrderItemDetailsDTO;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.repository.OrderRepository;
import com.team05.fooddelivery.order.repository.mongo.MongoOrderEventRepository;
import com.team05.fooddelivery.order.repository.neo4j.RestaurantNodeRepository;
import com.team05.fooddelivery.order.repository.neo4j.UserNodeRepository;
import com.team05.fooddelivery.order.model.neo4j.OrderedFrom;
import com.team05.fooddelivery.order.model.neo4j.RestaurantNode;
import com.team05.fooddelivery.order.model.neo4j.UserNode;
import com.team05.fooddelivery.order.dto.InteractionRecordingResponseDTO;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.model.mongo.OrderEvent.OrderEventActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final MongoOrderEventRepository mongoOrderEventRepository;
    private final UserNodeRepository userNodeRepository;
    private final OrderInteractionGraphService orderInteractionGraphService;

    public OrderService(OrderRepository orderRepository,
                        MongoOrderEventRepository mongoOrderEventRepository,
                        UserNodeRepository userNodeRepository,
                        OrderInteractionGraphService orderInteractionGraphService) {
        this.orderRepository = orderRepository;
        this.mongoOrderEventRepository = mongoOrderEventRepository;
        this.userNodeRepository = userNodeRepository;
        this.orderInteractionGraphService = orderInteractionGraphService;
        this.observers.add(
                new MongoEventLogger<>(this.mongoOrderEventRepository, EventType.ORDER)
        );
    }
    // [S3-F1] Search Orders by Status and Date Range
    @Cacheable(value = "order-service::S3-F1", key = "{#status, #startDate.toString(), #endDate.toString()}")
    public List<Order> searchOrders(OrderStatusEnum status, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.searchByStatusAndDateRange(
                status,
                startDateTime,
                endDateTimeExclusive
        );

        return orders;

    }
    // [S3-F2] Confirm Order and Assign Restaurant (Transactional)
    @Transactional
    @Caching(put = {
            @CachePut(value = "order-service::order", key = "#orderId"),
    }, evict = {
            @CacheEvict(value = "order-service::S3-F1", allEntries = true),
            @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#restaurantId")
    })
    public Order confirmOrderAndAssignRestaurant(Long orderId, Long restaurantId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatusEnum.PLACED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "only a newly placed order can be confirmed"
            );
        }
        boolean restaurantExists = orderRepository.existsByRestaurantId(restaurantId);
        if (!restaurantExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        boolean restaurantOpen = orderRepository.isRestaurantOpen(restaurantId);
        if (!restaurantOpen) {
            throw new ResponseStatusException( HttpStatus.BAD_REQUEST,  "Restaurant is not open"
            );
        }
        order.setRestaurantId(restaurantId);
        order.setStatus(OrderStatusEnum.CONFIRMED);


        Order savedOrder = orderRepository.save(order);
        Map<String, Object> params = new HashMap<>();
        params.put("order", savedOrder);
        params.put("orderId", savedOrder.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", savedOrder.getUserId());
        details.put("restaurantId", savedOrder.getRestaurantId());
        details.put("status", savedOrder.getStatus().name());
        details.put("assignedRestaurantId", restaurantId);

        params.put("details", details);

        notifyObservers(OrderEventActions.ORDER_CONFIRMED, params);
        return savedOrder;

    }
    // [S3-F3] Order Cost Estimation (Report DTO)
    @Transactional(readOnly = true)
    @Cacheable(value = "order-service::S3-F3", key = "{#request.restaurantId(), #request.itemCount(), #request.deliveryDistance()}")
    public OrderCostEstimateDTO estimateOrderCost(OrderEstimateRequest request) {
        if(request == null || request.restaurantId() == null || request.itemCount() == null || request.deliveryDistance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order estimate request");
        }
        if (request.itemCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemCount must be greater than 0");
        }
        if (request.deliveryDistance() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryDistance must be > 0");
        }
        boolean restaurantExists = orderRepository.existsByRestaurantId(request.restaurantId());
        if (!restaurantExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        Double avgMenuPrice = orderRepository.findAverageMenuItemPriceByRestaurantId(request.restaurantId());
        if (avgMenuPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant has no menu items right now");
        }
        double foodCost = avgMenuPrice * request.itemCount();
        double deliveryFee = 10.0 * request.deliveryDistance();
        double serviceFee = foodCost * 0.05;
        long activeOrders = orderRepository.countActiveOrdersByRestaurantId(request.restaurantId());
        double surgeMultiplier = activeOrders > 10 ? (activeOrders > 20 ? 1.5: 1.2) : 1.0;
        double total = (foodCost+deliveryFee + serviceFee) * surgeMultiplier;
        return OrderCostEstimateDTO.builder()
                .estimatedFoodCost(foodCost)
                .deliveryFee(deliveryFee)
                .serviceFee(serviceFee)
                .estimatedTotal(total)
                .surgeMultiplier(surgeMultiplier)
                .build();
    }
    // [S3-F4] Deliver Order
    @Transactional
    @Caching(put = {
            @CachePut(value = "order-service::order", key = "#id"),
    }, evict = {
            @CacheEvict(value = "order-service::S3-F1", allEntries = true),
            @CacheEvict(value = "order-service::S3-F3", allEntries = true),
            @CacheEvict(value = "order-service::S3-F9", key = "#id"),
    })
    public Order deliverOrder(Long id) {
        Order foundOrder = orderRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (foundOrder.getStatus() != OrderStatusEnum.PREPARING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order can only be delivered if it is in preparation");

        foundOrder.setStatus(OrderStatusEnum.DELIVERED);
        foundOrder.setDeliveredAt(LocalDateTime.now());

        if (foundOrder.getTotalAmount() == null) {
            List<OrderItem> orderItems = foundOrder.getOrderItems();
            double total = orderItems.stream().mapToDouble(i-> i.getQuantity() *i.getUnitPrice()).sum();
            foundOrder.setTotalAmount(total);
        }
        // Create payment record with status PENDING. Save order. Return the order after the update.
        orderRepository.createPaymentWithPendingStatus(foundOrder.getId(), foundOrder.getUserId(), foundOrder.getTotalAmount());
        Order savedOrder = orderRepository.save(foundOrder);
        Map<String, Object> params = new HashMap<>();
        params.put("order", savedOrder);
        params.put("orderId", savedOrder.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", savedOrder.getUserId());
        details.put("restaurantId", savedOrder.getRestaurantId());
        details.put("status", savedOrder.getStatus().name());
        details.put("deliveredAt", savedOrder.getDeliveredAt());
        details.put("totalAmount", savedOrder.getTotalAmount());

        params.put("details", details);

        notifyObservers(OrderEventActions.ORDER_DELIVERED, params);
        return savedOrder;


    }
    // [S3-F5] Filter Orders by Metadata
    @Transactional(readOnly = true)
    @Cacheable(value = "order-service::S3-F5", key = "{#key, #value}")
    public List<Order> searchOrdersByMetadata(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata key must not be empty");
        }

        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Metadata value must not be empty");
        }

        List<Order> orders = orderRepository.findByMetadataKeyValue(key.trim(), value.trim());

        for (Order order : orders) {// Redis complains about lazy loading of order items when trying to cache the orders, this is suboptimal. but works
            order.setOrderItems(new ArrayList<>(order.getOrderItems()));
        }

        // TODO: Add mongoEventLogging for this search action

        return orders;
    }
    // [S3-F6] - Order Analytics by Time Period (Report DTO)
    @Cacheable(value = "order-service::S3-F6", key = "{#startDate.toString(), #endDate.toString()}")
    public OrderAnalyticsDTO getOrderAnalyticsByTimePeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        Object[] analyticsData = orderRepository.getOrderAnalyticsByTimePeriod(startDateTime, endDateTimeExclusive)[0];
        
        OrderAnalyticsDTO analyticsObject = orderArrayToOrderAnalyticsDTOAdapter.adapt(analyticsData);

        return analyticsObject;
    }
    // [S3-F7] Cancel Order
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "order-service::order", key = "#orderId"),
            @CacheEvict(value = "order-service::S3-F1", allEntries = true),
            @CacheEvict(value = "order-service::S3-F3", allEntries = true),
            @CacheEvict(value = "order-service::S3-F6", allEntries = true),
            @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
            @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true),
            @CacheEvict(value = "delivery-service::S4-F10", allEntries = true)
    })
    public void cancelOrder(Long orderId) {
        //Get order through JPA default method findById, if order not found, throw HTTP 404 Not Found
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        //Check Status, if status != PLACED or CONFIRMED, throw HTTP 400 Bad Request
        if (!(order.getStatus() == OrderStatusEnum.PLACED || order.getStatus() == OrderStatusEnum.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be cancelled as it is already: " + order.getStatus());
        }
        //Update status to CANCELLED
        order.setStatus(OrderStatusEnum.CANCELLED);
        Map<String, Object> params = new HashMap<>();
        params.put("order", order);
        params.put("orderId", order.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", order.getUserId());
        details.put("restaurantId", order.getRestaurantId());
        details.put("status", order.getStatus().name());

        params.put("details", details);

        notifyObservers(OrderEventActions.ORDER_CANCELLED, params);
        //Update status of all order items to cancelled through repository
        // // // try {
        // // //     orderRepository.updateOrderItemsStatusByOrderId(orderId, OrderItemStatusEnum.CANCELLED);
        // // // } catch (Exception e) {
        // // //     System.err.println("Failed to cancel order items for orderId " + orderId + ": " + e.getMessage());
        // // //     throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to cancel order items: " + e.getMessage());
        // // // }
        //Call SQL query from within orderRepository to update deliveryStatus to CANCELLED
        try {
            orderRepository.cancelDeliveryByOrderId(orderId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Failed to cancel delivery: " + e.getMessage());
        }
    }
    // [S3-F8] Add items to existing order
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "order-service::order", key = "#orderId"),
            @CacheEvict(value = "order-service::S3-F1", allEntries = true),
            @CacheEvict(value = "order-service::S3-F3", allEntries = true),
            @CacheEvict(value = "order-service::S3-F6", allEntries = true),
            @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
            @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true),
    })
    public Order addItemsToOrder(Long orderId, List<OrderItem> orderItems) {
        Order existingOrder = getOrderById(orderId);
        int line_item_count = existingOrder.getOrderItems() != null ? existingOrder.getOrderItems().size() : 0;
        if (!(existingOrder.getStatus() == OrderStatusEnum.PLACED || existingOrder.getStatus() == OrderStatusEnum.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only add items to orders that are in PLACED or CONFIRMED status");
        }
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getQuantity() == null || orderItem.getItemName() == null || orderItem.getUnitPrice() == null || orderItem.getMenuItemId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order item must have quantity, item name, unit price and menu item id");
            }
            orderItem.setLineNumber(++line_item_count);
            orderItem.setStatus(OrderItemStatusEnum.PENDING);
            orderItem.setOrder(existingOrder);
        }
        existingOrder.getOrderItems().addAll(orderItems);
        Order returnObject = orderRepository.save(existingOrder);




        Map<String, Object> params = new HashMap<>();
        params.put("order", returnObject);
        params.put("orderId", returnObject.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", returnObject.getUserId());
        details.put("restaurantId", returnObject.getRestaurantId());
        details.put("status", returnObject
        .getStatus().name());
        details.put("totalItemsAfterAdd",
                returnObject.getOrderItems() != null ? returnObject.getOrderItems().size() : 0);
        
        params.put("details", details);

        notifyObservers(OrderEventActions.ITEMS_ADDED, params);

        return returnObject;
    }
    // [S3-F9] Get Order Details with items (Report DTO)
    @Transactional(readOnly = true)
    @Cacheable(value = "order-service::S3-F9", key = "#orderId")
    public OrderDetailsDTO getOrderDetails(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        List<OrderItem> orderItems = order.getOrderItems() != null
                ? new ArrayList<>(order.getOrderItems())
                : new ArrayList<>();

        orderItems.sort(Comparator.comparing(OrderItem::getLineNumber));

        List<OrderItemDetailsDTO> items = orderItems.stream()
                .map(item -> OrderItemDetailsDTO.builder()
                        .id(item.getId())
                        .lineNumber(item.getLineNumber())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .status(item.getStatus())
                        .metadata(item.getMetadata())
                        .build()
                )
                .toList();

        int totalItems = items.size();

        int preparedItems = (int) orderItems.stream()
                .filter(item -> item.getStatus() == OrderItemStatusEnum.PREPARED)
                .count();

        return OrderDetailsDTO.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .metadata(order.getMetadata())
                .items(items)
                .totalItems(totalItems)
                .preparedItems(preparedItems)
                .build();
    }
    // [S3-F10] Get Order Analytics Dashboard (Report DTO)
    // Cached in redis for 10 minutes
    @Transactional
    public OrderAnalyticsDashboardDTO getOrderAnalyticsDashboardWrapper(LocalDate startDate, LocalDate endDate) {
        OrderAnalyticsDashboardDTO dashboard = getOrderAnalyticsDashboard(startDate, endDate);

        Map<String, Object> params = new HashMap<>();
        params.put("orderId", -1L); // Aggregate logs with orderId -1
        
        Map<String, Object> details = new HashMap<>();
        details.put("startDate", startDate);
        details.put("endDate", endDate);
        details.put("analyticsDashboard", dashboard);

        notifyObservers("ANALYTICS_VIEWED", params);

        return dashboard;
    }
    @Transactional(readOnly = true)
    @Cacheable(value = "order-service::S3-F10", key = "{#startDate.toString(), #endDate.toString()}")
    public OrderAnalyticsDashboardDTO getOrderAnalyticsDashboard(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate query parameters are required");
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        Object[] result = orderRepository.getOrderCountAndCompletionRateDetails(startDateTime, endDateTimeExclusive)[0];

        OrderAnalyticsDashboardDTO analyticsDTO = ObjectArrayToOrderAnalyticsDashboardDTOAdapter.adapt(result);

        return analyticsDTO;
    }
    // [S3-F11] Record User-Restaurant Ordering Pattern
    public InteractionRecordingResponseDTO recordInteraction(Long orderId) {
        // (a) Validate JWT + USER role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean isUser = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER") || role.equals("USER") || role.equals("ROLE_CUSTOMER") || role.equals("CUSTOMER"));

        if (!isUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only users can record interactions");
        }

        // (b)
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatusEnum.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interactions can only be recorded for Delivered orders");
        }

        Long userId = order.getUserId();
        Long restaurantId = order.getRestaurantId();
        if (userId == null || restaurantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order is missing user or restaurant association");
        }
        // (d)
        if (userNodeRepository.isOrderRecordedInRelationship(userId, restaurantId, orderId)) {
            return new InteractionRecordingResponseDTO(
                    orderId, userId, restaurantId,
                    "Interaction already recorded; no-op", true);
        }

        // (e)
        String userName = orderRepository.findUserNameById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String restaurantName = orderRepository.findRestaurantNameById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        String cuisineType = orderRepository.findRestaurantCuisineTypeById(restaurantId).orElse(null);

        // (f)
        boolean lostIdempotencyRace = orderInteractionGraphService.upsertOrderedFromEdge(
                orderId, userId, userName, restaurantId, restaurantName, cuisineType);
        if (lostIdempotencyRace) {
            return new InteractionRecordingResponseDTO(
                    orderId, userId, restaurantId,
                    "Interaction already recorded; no-op", true);
        }

        // (g)
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", orderId);
        details.put("userId", userId);
        details.put("restaurantId", restaurantId);
        params.put("details", details);
        notifyObservers(OrderEventActions.INTERACTION_RECORDED, params);

        // (h)
        return new InteractionRecordingResponseDTO(
                orderId, userId, restaurantId,
                "Interaction recorded", false);

    }

    // [CRUD]
    //// Get order by ID
    @Transactional(readOnly = true)
    @Cacheable(value = "order-service::order", key = "#orderId")
    public Order getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setOrderItems(new ArrayList<>(order.getOrderItems()));
        return order;
    }
    //// Get all orders
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        List<Order> allOrders = orderRepository.findAll();
        return allOrders;
    }
    //// Create order
    @Transactional
    @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true)
    public Order createOrder(Order order) {
        boolean userExists = orderRepository.existsByUserId(order.getUserId());
        if (!userExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }
        if(order.getRestaurantId() != null){
            boolean restaurantExists = orderRepository.existsByRestaurantId(order.getRestaurantId());
            if (!restaurantExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant not found");
        }
        }


        Order savedOrder = orderRepository.save(order);
        Map<String, Object> params = new HashMap<>();
        params.put("order", savedOrder);
        params.put("orderId", savedOrder.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("orderId", savedOrder.getId());
        details.put("userId", savedOrder.getUserId());
        details.put("restaurantId", savedOrder.getRestaurantId());
        details.put("status", savedOrder.getStatus() != null ? savedOrder.getStatus().name() : null);
        details.put("totalAmount", savedOrder.getTotalAmount());

        params.put("details", details);

        notifyObservers(OrderEventActions.ORDER_CREATED, params);
        return savedOrder;
    }
    //// Update order
    @Transactional
    @Caching(put = {
            @CachePut(value = "order-service::order", key = "#orderId"),
    }, evict = {
            @CacheEvict(value = "order-service::S3-F1", allEntries = true),
            @CacheEvict(value = "order-service::S3-F3", allEntries = true),
            @CacheEvict(value = "order-service::S3-F5", allEntries = true),
            @CacheEvict(value = "order-service::S3-F6", allEntries = true),
            @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
            @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true)

    })
    public Order updateOrder(Long orderId, Order updatedOrder) {
        Order existingOrder = getOrderById(orderId);

        if (updatedOrder.getDeliveryAddress() != null) {
            existingOrder.setDeliveryAddress(updatedOrder.getDeliveryAddress());
        }

        if (updatedOrder.getTotalAmount() != null) {
            existingOrder.setTotalAmount(updatedOrder.getTotalAmount());
        }

        if (updatedOrder.getRestaurantId() != null){
            boolean restaurantExists = orderRepository.existsByRestaurantId(updatedOrder.getRestaurantId());
            if (!restaurantExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant you're trying to set does not exist");
            }

            existingOrder.setRestaurantId(updatedOrder.getRestaurantId());
        }

        if (updatedOrder.getMetadata() != null) {
            existingOrder.setMetadata(updatedOrder.getMetadata());
        }

        if (updatedOrder.getStatus() != null) {
            existingOrder.setStatus(updatedOrder.getStatus());
        }

        if (updatedOrder.getDeliveredAt() != null) {
            existingOrder.setDeliveredAt(updatedOrder.getDeliveredAt());
        }

        Order savedOrder = orderRepository.save(existingOrder);
        Map<String, Object> params = new HashMap<>();
        params.put("order", savedOrder);
        params.put("orderId", savedOrder.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", savedOrder.getUserId());
        details.put("restaurantId", savedOrder.getRestaurantId());
        details.put("status", savedOrder.getStatus() != null ? savedOrder.getStatus().name() : null);
        details.put("updatedFields", updatedOrder);

        params.put("details", details);

        notifyObservers(OrderEventActions.ORDER_UPDATED, params);
        return savedOrder;
    }
    //// Delete order
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "order-service::order", key = "#orderId"),
            @CacheEvict(value = "order-service::S3-F1", allEntries = true),
            @CacheEvict(value = "order-service::S3-F3", allEntries = true),
            @CacheEvict(value = "order-service::S3-F5", allEntries = true),
            @CacheEvict(value = "order-service::S3-F6", allEntries = true),
            @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
            @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true)
    })
    public void deleteOrder(Long orderId) {
        Order existingOrder = getOrderById(orderId);
        orderRepository.delete(existingOrder);
        Map<String, Object> params = new HashMap<>();
        params.put("order", existingOrder);
        params.put("orderId", existingOrder.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", existingOrder.getUserId());
        details.put("restaurantId", existingOrder.getRestaurantId());
        details.put("statusBeforeDelete",
                existingOrder.getStatus() != null ? existingOrder.getStatus().name() : null);

        params.put("details", details);

        notifyObservers(OrderEventActions.ORDER_DELETED, params);
    }

    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
}