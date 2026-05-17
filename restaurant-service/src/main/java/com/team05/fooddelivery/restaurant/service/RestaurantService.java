package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.contracts.dto.AvgPriceDTO;
import com.team05.fooddelivery.contracts.dto.RestaurantOrderSummaryDTO;
import com.team05.fooddelivery.contracts.events.RestaurantRatedEvent;
import com.team05.fooddelivery.contracts.events.RestaurantStatusChangedEvent;
import com.team05.fooddelivery.contracts.feign.OrderServiceClient;
import com.team05.fooddelivery.restaurant.adapter.TopRestaurantAdapter;
import com.team05.fooddelivery.restaurant.dto.RestaurantDashboardDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantMenuAlertDTO;
import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.team05.fooddelivery.restaurant.messaging.publishers.RestaurantEventPublisher;
import com.team05.fooddelivery.restaurant.model.MenuItem;
import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.repository.MenuItemRepository;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import com.team05.fooddelivery.restaurant.repository.mongo.MongoRestaurantEventRepository;
import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.model.mongo.RestaurantEvent.RestaurantEventActions;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
// [S2-F10] ES document class returned by full-text search
import com.team05.fooddelivery.restaurant.model.elasticsearch.RestaurantSearchDocument;
// [S2-F10] Spring Data ES operations for building native queries
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
// [S2-F10] Elastic client query DSL types for bool/multiMatch/term/range
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import feign.FeignException;

import com.team05.fooddelivery.contracts.dto.OrderDTO;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final CacheManager cacheManager;
    private final RestaurantElasticsearchIndexService restaurantElasticsearchIndexService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RestaurantEventPublisher restaurantEventPublisher;
    private final OrderServiceClient orderServiceClient;

    public RestaurantService(RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            MongoRestaurantEventRepository mongoRestaurantEventRepository,
            RestaurantElasticsearchIndexService restaurantElasticsearchIndexService,
            CacheManager cacheManager,
            ElasticsearchOperations elasticsearchOperations,
            RestaurantEventPublisher restaurantEventPublisher,
            OrderServiceClient orderServiceClient) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.cacheManager = cacheManager;
        this.restaurantElasticsearchIndexService = restaurantElasticsearchIndexService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.restaurantEventPublisher = restaurantEventPublisher;
        this.orderServiceClient = orderServiceClient;
        this.observers.add(
                new MongoEventLogger<>(mongoRestaurantEventRepository, EventType.RESTAURANT));
    }

    // CRUD create — no cache eviction (spec Section 4.4.4)
    public Restaurant create(Restaurant restaurant) {
        if (restaurant.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant must not have an id");
        }

        // Section 7.2 — ensure details.description key exists with default empty string
        Map<String, Object> details = restaurant.getDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        details.putIfAbsent("description", "");
        restaurant.setDetails(details);

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("{} {} saved with status={}", "Restaurant", saved.getId(), saved.getStatus());

        // Notify observers — Section 4.5
        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", saved.getId());
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("name", saved.getName());
        eventDetails.put("cuisineType", saved.getCuisineType());
        params.put("details", eventDetails);
        notifyObservers(RestaurantEventActions.RESTAURANT_CREATED, params);
        // s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(saved);

        return saved;
    }

    // Cached 15 min — spec Section 4.4.2
    @Cacheable(value = "restaurant-service::restaurant", key = "#id")
    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    }

    // NOT cached — list endpoints never cached, spec Section 4.4.2
    public List<Restaurant> getAll() {
        return restaurantRepository.findAll();
    }

    // CRUD update — evict caches + notify observers
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F10", allEntries = true)
    })

    public Restaurant update(Long id, Restaurant updated) {
        Restaurant existing = getById(id);
        if (updated.getName() != null)
            existing.setName(updated.getName());
        if (updated.getEmail() != null)
            existing.setEmail(updated.getEmail());
        if (updated.getPhone() != null)
            existing.setPhone(updated.getPhone());
        if (updated.getCuisineType() != null)
            existing.setCuisineType(updated.getCuisineType());
        if (updated.getStatus() != null)
            existing.setStatus(updated.getStatus());
        if (updated.getDetails() != null)
            existing.setDetails(updated.getDetails());
        Restaurant saved = restaurantRepository.save(existing);
        log.info("{} {} saved with status={}", "Restaurant", saved.getId(), saved.getStatus());

        Map<String, Object> params = new HashMap<>();
        params.put("action", RestaurantEventActions.UPDATED);
        params.put("restaurantId", saved.getId());
        Map<String, Object> details = new HashMap<>();
        details.put("name", saved.getName());
        details.put("status", saved.getStatus());
        params.put("details", details);
        notifyObservers(RestaurantEventActions.UPDATED, params);
        // s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(saved);
        return saved;
    }

    // CRUD delete — evict caches + notify observers
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F10", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id")
    })
    public void delete(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        restaurantRepository.deleteById(id);
        // s2-f11
        restaurantElasticsearchIndexService.deleteByRestaurantId(id);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", id);
        params.put("details", new HashMap<>());
        notifyObservers(RestaurantEventActions.RESTAURANT_DELETED, params);
    }

    // [S2-F1] Cached 5 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F1", key = "#cuisineType + ':' + #minRating + ':' + #maxRating")
    public List<Restaurant> searchByCuisineAndRating(String cuisineType, Double minRating, Double maxRating) {
        if (minRating > maxRating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRating cannot be greater than maxRating");
        }
        return restaurantRepository.searchByCuisineAndRating(cuisineType, minRating, maxRating);
    }

    // [S2-F2] Write — invalidates caches + notify observers — Section 4.4.4
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F10", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id")
    })
    public Restaurant updateDetails(Long id, Map<String, Object> newDetails) {
        Restaurant existing = getById(id);
        Map<String, Object> currentDetails = existing.getDetails();
        if (currentDetails == null) {
            existing.setDetails(newDetails);
        } else {
            currentDetails.putAll(newDetails);
            existing.setDetails(currentDetails);
        }
        Restaurant saved = restaurantRepository.save(existing);
        log.info("{} {} saved with status={}", "Restaurant", saved.getId(), saved.getStatus());
        // s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(saved);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", saved.getId());
        params.put("details", newDetails);
        notifyObservers(RestaurantEventActions.DETAILS_UPDATED, params);

        return saved;
    }

    // [S2-F3] M3: Feign → order-service. Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F3", key = "#id")
    public RestaurantRevenueDTO getOrderSummary(Long id) {
        Restaurant restaurant = getById(id);
        RestaurantOrderSummaryDTO summary = fetchOrderSummaryOrEmpty(id);
        return RestaurantRevenueDTO.builder()
                .restaurantId(restaurant.getId())
                .name(restaurant.getName())
                .totalOrders(summary.totalOrders())
                .totalRevenue(summary.totalRevenue().doubleValue())
                .averageOrderAmount(summary.avgOrderValue().doubleValue())
                .build();
    }

    // [S2-F4] Write — invalidates caches + notify observers — Section 4.4.4
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#id"),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F10", allEntries = true)
    })
    public void updateRestaurantStatus(Long id, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        // CLOSE added for TC 246
        if ("SUSPENDED".equals(newStatus) || "CLOSED".equals(newStatus)) {
            int activeOrders = fetchActiveOrderCountOrZero(id);
            if (activeOrders > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot suspend or close restaurant with active orders");
            }
        }

        String oldStatus = restaurant.getStatus().toString();

        try {
            restaurant.setStatus(RestaurantStatusEnum.valueOf(newStatus));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatus);
        }
        restaurantRepository.save(restaurant);
        log.info("{} {} saved with status={}", "Restaurant", restaurant.getId(), restaurant.getStatus());
        // s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(restaurant);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", id);
        Map<String, Object> details = new HashMap<>();
        details.put("newStatus", newStatus);
        params.put("details", details);
        notifyObservers(RestaurantEventActions.STATUS_CHANGED, params);
        restaurantEventPublisher.publishStatusChanged(new RestaurantStatusChangedEvent(id, oldStatus, newStatus));
    }

    // [S2-F5] Cached 5 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F5", key = "#key + ':' + #value + ':' + #status")
    public List<Restaurant> filterByDetail(String key, String value, String status) {
        return restaurantRepository.findByDetailAttribute(key, value, status);
    }

    // [S2-F6] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F6", key = "#limit")
    public List<TopRestaurantDTO> getTopRated(int limit) {
        List<Object[]> results = restaurantRepository.findTopRatedRestaurants(limit);
        List<TopRestaurantDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            dtos.add(new TopRestaurantAdapter(row).adapt(row));
        }
        return dtos;
    }

    // [S2-F7] M3: Replace cross-DB order validation with Feign → order-service
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant-service::restaurant", key = "#restaurantId"),
            @CacheEvict(value = "restaurant-service::S2-F1", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F3", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F5", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F6", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F9", allEntries = true),
            @CacheEvict(value = "restaurant-service::S2-F12", key = "#restaurantId"),
            @CacheEvict(value = "restaurant-service::S2-F10", allEntries = true)
    })
    public void rateRestaurant(Long restaurantId, Long orderId, Double rating) {
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must not be null");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }
        Restaurant rest = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        // M3: Validate order via Feign → order-service GET /api/orders/{orderId}
        OrderDTO order = fetchOrderOrThrow(orderId);
        if (!order.restaurantId().equals(restaurantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order does not belong to this restaurant");
        }
        if (!"DELIVERED".equals(order.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not delivered");
        }

        int newTRating = rest.getTotalRatings() + 1;
        double newRating = ((rest.getRating() * rest.getTotalRatings()) + rating) / newTRating;
        rest.setRating(newRating);
        rest.setTotalRatings(newTRating);
        restaurantRepository.save(rest);
        log.info("{} {} saved with status={}", "Restaurant", rest.getId(), rest.getStatus());
        // s2-f11
        restaurantElasticsearchIndexService.upsertFromRestaurant(rest);

        Map<String, Object> params = new HashMap<>();
        params.put("restaurantId", restaurantId);
        Map<String, Object> details = new HashMap<>();
        details.put("rating", rating);
        details.put("orderId", orderId);
        params.put("details", details);
        notifyObservers(RestaurantEventActions.REVIEW_ADDED, params);
        restaurantEventPublisher.publishRated(new RestaurantRatedEvent(restaurantId, orderId, rating, null));
    }

    // [S2-F9] Cached 10 min — Section 4.4.1
    @Cacheable(value = "restaurant-service::S2-F9", key = "'all'")
    public List<RestaurantMenuAlertDTO> getRestaurantsWithUnavailableItems() {
        List<Restaurant> restaurants = restaurantRepository.findRestaurantsWithUnavailableItems();
        List<RestaurantMenuAlertDTO> dtos = new ArrayList<>();
        for (Restaurant r : restaurants) {
            List<MenuItem> unavailableItems = menuItemRepository.findByRestaurantIdAndAvailable(r.getId(), false);
            dtos.add(RestaurantMenuAlertDTO.builder()
                    .restaurantId(r.getId())
                    .restaurantName(r.getName())
                    .restaurantStatus(r.getStatus().toString())
                    .unavailableItems(unavailableItems)
                    .unavailableCount(unavailableItems.size())
                    .build());
        }
        return dtos;
    }

    // s2-f11
    public void indexRestaurantForSearch(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        restaurantElasticsearchIndexService.upsertFromRestaurant(restaurant);
    }

    // S2-F10 Full-Text Restaurant Search — spec §10.2.1
    // Searches Elasticsearch on name and description with optional filters.
    // Cached for 5 minutes in Redis (cache key includes all params).
    @Cacheable(value = "restaurant-service::S2-F10", key = "'f10:' + #query + '|' + (#cuisineType != null ? #cuisineType : '_') + '|' + (#status != null ? #status : '_') + '|' + (#minRating != null ? #minRating.toString() : '_') + '|' + (#maxRating != null ? #maxRating.toString() : '_')")
    public List<RestaurantSearchDocument> fullTextSearch(
            String query,
            String cuisineType,
            String status,
            Double minRating,
            Double maxRating) {

        // Start building a bool query — all filters go as "filter" clauses (do not
        // affect score)
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // must clause: full-text match on both name and description fields
        // fuzziness AUTO handles partial matches and case-insensitivity
        boolQuery.must(Query.of(q -> q
                .multiMatch(m -> m
                        .query(query) // the search text provided by the user
                        .fields("name", "description") // search across both text fields
                        .fuzziness("AUTO") // AUTO fuzziness enables partial/case-insensitive matching
                )));
        // optional filter: cuisineType exact match (keyword field — case-sensitive enum
        // value)
        if (cuisineType != null && !cuisineType.isBlank()) {
            boolQuery.filter(Query.of(q -> q
                    .term(t -> t.field("cuisineType.keyword").value(cuisineType))));
        }
        // optional filter: status exact match (keyword field — OPEN / CLOSED /
        // SUSPENDED)
        if (status != null && !status.isBlank()) {
            boolQuery.filter(Query.of(q -> q
                    .term(t -> t.field("status.keyword").value(status))));
        }
        // optional filter: rating range (double field — min and/or max, both optional)
        if (minRating != null || maxRating != null) {
            boolQuery.filter(Query.of(q -> q
                    .range(r -> {
                        r.number(n -> {
                            n.field("rating"); // target the rating double field
                            if (minRating != null)
                                n.gte(minRating); // greater than or equal
                            if (maxRating != null)
                                n.lte(maxRating); // less than or equal
                            return n;
                        });
                        return r;
                    })));
        }

        // Wrap the bool query inside a NativeQuery that Spring Data ES can execute
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQuery.build())))
                .build();
        // Execute the query — returns SearchHits ordered by relevance score (highest
        // first)
        SearchHits<RestaurantSearchDocument> hits = elasticsearchOperations.search(nativeQuery,
                RestaurantSearchDocument.class);
        // Map each SearchHit wrapper to the inner document and collect to a plain list
        return hits.stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
    }

    // [S2-F12] Get Restaurant Performance Dashboard
    // M3: order aggregation via Feign → order-service (reuses S2-F3 endpoint)
    // Section 10.2.3 — cached 10 min
    public RestaurantDashboardDTO getDashboard(Long id) {
        // Step 1 — logging ALWAYS runs (even on cache hits)
        notifyDashboardViewed(id);

        // Step 2 — check cache manually
        Cache cache = cacheManager.getCache("restaurant-service::S2-F12");
        if (cache != null) {
            RestaurantDashboardDTO cached = cache.get(id, RestaurantDashboardDTO.class);
            if (cached != null) {
                return cached; // cache hit
            }
        }

        // Step 3 — cache miss — fetch from DB + Feign
        long start = System.currentTimeMillis();
        Restaurant restaurant = getById(id);
        RestaurantOrderSummaryDTO summary = fetchOrderSummaryOrEmpty(id);
        Long activeMenuItems = restaurantRepository.countActiveMenuItems(id);

        RestaurantDashboardDTO dto = RestaurantDashboardDTO.builder()
                .restaurantId(restaurant.getId())
                .name(restaurant.getName())
                .totalOrders(summary.totalOrders())
                .totalRevenue(summary.totalRevenue().doubleValue())
                .averageOrderValue(summary.avgOrderValue().doubleValue())
                .activeMenuItems(activeMenuItems)
                .build();

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 1000) {
            log.warn("Slow {} took {}ms", "getDashboard", elapsed);
        }

        // Step 4 — store in cache
        if (cache != null) {
            cache.put(id, dto);
        }

        return dto;
    }

    // [S2-F12] Logs DASHBOARD_VIEWED event to MongoDB — called on every request
    // including cache hits
    // Section 10.2.3 — pure observability, does NOT invalidate cache
    public void notifyDashboardViewed(Long restaurantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", RestaurantEventActions.DASHBOARD_VIEWED);
        params.put("restaurantId", restaurantId);
        params.put("details", new HashMap<>());
        notifyObservers(RestaurantEventActions.DASHBOARD_VIEWED, params);
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

    // [S2-READ-DB] Calculates average price of available menu items for a restaurant, used by order-service via Feign. Not cached at this level since it's called by order-service which has its own caching requirements.
    public AvgPriceDTO getMenuItemsAvgPrice(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        BigDecimal avg = menuItemRepository.findAvgPriceByRestaurantId(id);
        return new AvgPriceDTO(avg);
    }


    private RestaurantOrderSummaryDTO fetchOrderSummaryOrEmpty(Long restaurantId) {
        try {
            log.info("Calling order-service.getRestaurantOrderSummary with restaurantId={}", restaurantId);
            RestaurantOrderSummaryDTO summary = orderServiceClient.getRestaurantOrderSummary(restaurantId);
            log.info("order-service.getRestaurantOrderSummary returned successfully");
            return summary;
        } catch (FeignException.NotFound ex) {
            log.info("order-service returned 404 for restaurantId={} — returning empty summary", restaurantId);
            return RestaurantOrderSummaryDTO.empty();
        } catch (FeignException ex) {
            log.warn("Feign call to order-service failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service temporarily unavailable", ex);
        }
    }

    private int fetchActiveOrderCountOrZero(Long restaurantId) {
        try {
            log.info("Calling order-service.getActiveOrderCountByRestaurant with restaurantId={}", restaurantId);
            int count = orderServiceClient.getActiveOrderCountByRestaurant(restaurantId);
            log.info("order-service.getActiveOrderCountByRestaurant returned successfully");
            return count;
        } catch (FeignException.NotFound ex) {
            log.info("order-service returned 404 for restaurantId={} — treating as 0 active orders", restaurantId);
            return 0;
        } catch (FeignException ex) {
            log.warn("Feign call to order-service failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service temporarily unavailable", ex);
        }
    }

    private OrderDTO fetchOrderOrThrow(Long orderId) {
        try {
            log.info("Calling order-service.getOrder with orderId={}", orderId);
            OrderDTO order = orderServiceClient.getOrder(orderId);
            log.info("order-service.getOrder returned successfully");
            return order;
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with id: " + orderId, ex);
        } catch (FeignException ex) {
            log.warn("Feign call to order-service failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service temporarily unavailable", ex);
        }
    }

}