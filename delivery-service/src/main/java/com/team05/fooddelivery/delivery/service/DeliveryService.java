package com.team05.fooddelivery.delivery.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.team05.fooddelivery.delivery.adapter.CassandraRowAdapter;
import com.team05.fooddelivery.delivery.dto.*;
import com.team05.fooddelivery.delivery.enums.DeliveryStatus;
import com.team05.fooddelivery.delivery.model.Delivery;
import com.team05.fooddelivery.delivery.model.cassandra.DeliveryTrackingEvent;
import com.team05.fooddelivery.delivery.repository.DeliveryRepository;
import com.team05.fooddelivery.delivery.repository.cassandra.DeliveryTrackingEventRepository;
import com.team05.shared.model.mongo.MongoEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.team05.fooddelivery.delivery.repository.mongo.DeliveryEventRepository;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;

@Service
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryTrackingEventRepository trackingEventRepository;
    private final CassandraRowAdapter cassandraRowAdapter = new CassandraRowAdapter();
    private final CacheManager cacheManager;
    private final List<EntityObserver> observers = new ArrayList<>();

    public DeliveryService(DeliveryRepository deliveryRepository,CacheManager cacheManager,
                           DeliveryTrackingEventRepository trackingEventRepository,
                           DeliveryEventRepository eventRepository) {
        this.deliveryRepository = deliveryRepository;
        this.cacheManager = cacheManager;
        this.trackingEventRepository = trackingEventRepository;
        this.observers.add(
                new MongoEventLogger<>(eventRepository, MongoEvent.EventType.DELIVERY)
        );
    }

    /**
     * [S4-F2] Create Delivery Record with Metadata
     * Endpoint: POST /api/deliveries/order/{orderId}
     * Logs: DELIVERY_CREATED event with full delivery metadata
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F10", allEntries = true)
    })
    public Delivery createOrderDelivery(Long orderId, Delivery delivery) {
        if (!deliveryRepository.orderExists(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (delivery.getDriverName() == null || delivery.getDriverName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "driverName is required");
        }
        if (delivery.getLatitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude is required");
        }
        if (delivery.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longitude is required");
        }
        delivery.setOrderId(orderId);
        if (delivery.getMetadata() == null) {
            delivery.setMetadata(new HashMap<>());
        }
        if (delivery.getStatus() == null) {
            delivery.setStatus(DeliveryStatus.ASSIGNED);
        }
        Delivery saved = deliveryRepository.save(delivery);

        // [S4-F2] DELIVERY_CREATED: Notify observers to log event
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("orderId", orderId);
        eventDetails.put("driverName", saved.getDriverName());
        eventDetails.put("status", saved.getStatus());
        eventDetails.put("coordinates", Map.of("latitude", saved.getLatitude(), "longitude", saved.getLongitude()));
        if (saved.getMetadata() != null) {
            eventDetails.put("metadata", saved.getMetadata());
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("deliveryId", saved.getId());
        eventPayload.put("details", eventDetails);

        notifyObservers("DELIVERY_CREATED", eventPayload);

        return saved;
    }

    /**
     * Create Delivery (CRUD)
     * Basic creation without order context
     */
    public Delivery createDelivery(Delivery delivery) {
        if (delivery.getMetadata() == null) {
            delivery.setMetadata(new HashMap<>());
        }
        if (delivery.getStatus() == null) {
            delivery.setStatus(DeliveryStatus.ASSIGNED);
        }

        Delivery saved = deliveryRepository.save(delivery);

        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("orderId", saved.getOrderId());
        eventDetails.put("driverName", saved.getDriverName());
        eventDetails.put("status", saved.getStatus());
        eventDetails.put("coordinates", Map.of("latitude", saved.getLatitude(), "longitude", saved.getLongitude()));
        if (saved.getMetadata() != null) {
            eventDetails.put("metadata", saved.getMetadata());
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("deliveryId", saved.getId());
        eventPayload.put("details", eventDetails);


        notifyObservers("DELIVERY_CREATED", eventPayload);

        return saved;
    }

    /**
     * [S4-F11] Record Delivery Status Event
     * Endpoint: POST /api/deliveries/{id}/tracking
     */
    public void recordDeliveryTracking(Long deliveryId, DeliveryTrackingRequestDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (request.status() == null || request.status().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (request.latitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude is required");
        }
        if (request.longitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longitude is required");
        }

        DeliveryStatus status;
        try {
            status = DeliveryStatus.valueOf(request.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Delivery delivery = getDeliveryById(deliveryId);

        DeliveryTrackingEvent trackingEvent = new DeliveryTrackingEvent(
                deliveryId,
                Instant.now(),
                status.name(),
                delivery.getDriverName(),
                request.latitude(),
                request.longitude(),
                request.notes()
        );
        trackingEventRepository.save(trackingEvent);

        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("status", status.name());
        eventDetails.put("deliveryId", deliveryId);
        eventDetails.put("driverName", delivery.getDriverName());
        eventDetails.put("coordinates", Map.of("latitude", request.latitude(), "longitude", request.longitude()));
        if (request.notes() != null && !request.notes().isBlank()) {
            eventDetails.put("notes", request.notes());
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("deliveryId", deliveryId);
        eventPayload.put("details", eventDetails);

        // Mongo logging is observational and must not block the Cassandra write path.
        notifyObservers("TRACKING_RECORDED", eventPayload);
    }

    /**
     * [CRUD Read] Get Delivery by ID
     * Retrieve a single delivery record
     */
    @Cacheable(cacheNames = "delivery-service::delivery", key = "#id")
    @Transactional(readOnly = true)
    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

    /**
     * [CRUD Read] Get All Deliveries (optionally filtered by status)
     */
    @Transactional(readOnly = true)
    public List<Delivery> getAllDeliveries(String status) {
        if (status == null || status.isBlank()) {
            return deliveryRepository.findAll();
        }
        return deliveryRepository.findByStatus(status);
    }

    /**
     * [S4-F1] Get Latest Delivery for an Order
     * Endpoint: GET /api/deliveries/order/{orderId}/latest
     */
    @Cacheable(cacheNames = "delivery-service::S4-F1", key = "#orderId")
    @Transactional(readOnly = true)
    public Delivery getLatestDeliveryByOrderId(Long orderId) {
        validateOrder(orderId);
        return deliveryRepository.findLatestByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

    /**
     * [S4-F5] Filter Deliveries by Metadata (JSONB Query)
     * Endpoint: GET /api/deliveries/metadata/search?key={k}&operator={op}&value={v}
     * Operators: eq, gt, lt.
     */
    @Cacheable(cacheNames = "delivery-service::S4-F5", key = "#key + ':' + #operator + ':' + #value")
    @Transactional(readOnly = true)
    public List<Delivery> searchDeliveriesByMetadata(String key, String operator, String value) {
        if (operator == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operator");
        }

        String normalizedOperator = operator.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedOperator) {
            case "eq" -> deliveryRepository.findByMetadataEquals(key, value);
            case "gt" -> deliveryRepository.findByMetadataGreaterThan(key, value);
            case "lt" -> deliveryRepository.findByMetadataLessThan(key, value);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operator");
        };
    }

    /**
     * [CRUD Update] Update Delivery
     * Logs: DELIVERY_UPDATED event via Observer
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::delivery", key = "#id"),
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F5", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F10", allEntries = true)
    })
    public Delivery updateDelivery(Long id, Delivery delivery) {
        Delivery existingDelivery = getDeliveryById(id);

        if (delivery.getOrderId() != null) {
            existingDelivery.setOrderId(delivery.getOrderId());
        }
        if (delivery.getDriverName() != null) {
            existingDelivery.setDriverName(delivery.getDriverName());
        }
        if (delivery.getLatitude() != null) {
            existingDelivery.setLatitude(delivery.getLatitude());
        }
        if (delivery.getLongitude() != null) {
            existingDelivery.setLongitude(delivery.getLongitude());
        }
        if (delivery.getStatus() != null) {
            existingDelivery.setStatus(delivery.getStatus());
        }
        if (delivery.getMetadata() != null) {
            existingDelivery.setMetadata(delivery.getMetadata());
        }

        Delivery saved = deliveryRepository.save(existingDelivery);

        // [CRUD] DELIVERY_UPDATED: Notify observers to log event
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("deliveryId", id);
        eventDetails.put("updatedFields", Map.of(
                "driverName", saved.getDriverName(),
                "status", saved.getStatus(),
                "coordinates", Map.of("latitude", saved.getLatitude(), "longitude", saved.getLongitude())
        ));

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("deliveryId", saved.getId());
        eventPayload.put("details", eventDetails);

        notifyObservers("DELIVERY_UPDATED", eventPayload);

        return saved;
    }

    /**
     * [CRUD Delete] Delete Delivery
     * Logs: DELIVERY_DELETED event via Observer
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::delivery", key = "#id"),
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F5", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true), @CacheEvict(cacheNames = "delivery-service::S4-F10", allEntries = true)
    })
    public void deleteDelivery(Long id) {
        if (!deliveryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found");
        }
        // delete first, then notify observers. The MongoEventLogger swallows exceptions, but
        // ordering the DB change before the optional Mongo write is safer and avoids surprising
        // interactions if the observer behavior changes in future.
        deliveryRepository.deleteById(id);

        // [CRUD] DELIVERY_DELETED: Notify observers to log event
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("deletedDeliveryId", id);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("deliveryId", id);
        eventPayload.put("details", eventDetails);

        notifyObservers("DELIVERY_DELETED", eventPayload);
    }

    /**
     * [S4-F4] Batch Delivery Updates (Transactional)
     * Endpoint: POST /api/deliveries/batch
     * Logs: BATCH_STATUS_UPDATED event with count and delivery IDs
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true), @CacheEvict(cacheNames = "delivery-service::S4-F10", allEntries = true)
    })
    public int batchCreate(BatchDeliveryRequestDTO request) {
        if (!deliveryRepository.orderExists(request.getOrderId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (request.getDeliveries() == null || request.getDeliveries().isEmpty()) {
            return 0;
        }

        List<Delivery> toSave = new ArrayList<>();
        for (DeliveryItemDTO item : request.getDeliveries()) {
            if (item.getLatitude() == null || item.getLatitude() < -90 || item.getLatitude() > 90) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90");
            }
            if (item.getLongitude() == null || item.getLongitude() < -180 || item.getLongitude() > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180");
            }

            Delivery delivery = new Delivery();
            delivery.setOrderId(request.getOrderId());
            delivery.setDriverName(item.getDriverName());
            delivery.setLatitude(item.getLatitude());
            delivery.setLongitude(item.getLongitude());
            delivery.setStatus(item.getStatus() != null ? item.getStatus() : DeliveryStatus.ASSIGNED);
            delivery.setMetadata(item.getMetadata() != null ? item.getMetadata() : new HashMap<>());
            toSave.add(delivery);
        }

        deliveryRepository.saveAll(toSave);
        int count = toSave.size();

        // [S4-F4] BATCH_STATUS_UPDATED: Notify observers to log event
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("orderId", request.getOrderId());
        eventDetails.put("batchSize", count);
        eventDetails.put("deliveryIds", toSave.stream().map(Delivery::getId).collect(Collectors.toList()));

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("deliveryId", request.getOrderId());
        eventPayload.put("details", eventDetails);

        notifyObservers("BATCH_STATUS_UPDATED", eventPayload);

        return count;
    }

    /**
     * Observer notification helper
     * Propagates events to all registered observers (MongoEventLogger)
     */
    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    /**
     * Register an observer for this service's events.
     * Tests and other runtime components may use this to add/remove observers.
     */
    public void registerObserver(EntityObserver observer) {
        this.observers.add(observer);
    }

    /**
     * Unregister a previously registered observer.
     */
    public void unregisterObserver(EntityObserver observer) {
        this.observers.remove(observer);
    }

    // Accepts "HH:mm" (e.g. "14:10") or full ISO-8601 (e.g. "2024-01-01T14:10:00Z").
    // HH:mm is resolved against today's date in UTC — matches spec test scenario format.
    private Instant parseTimeParam(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            LocalTime time = LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDate.now(ZoneOffset.UTC).atTime(time).toInstant(ZoneOffset.UTC);
        }
    }

    private void validateOrder(Long orderId) {
        if (!deliveryRepository.orderExists(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    /**
     * [S4-F6] Order Delivery History in Date Range
     * Endpoint: GET /api/deliveries/order/{orderId}/history?startDate={d}&endDate={d}
     * Verify order (throws 404 if not found). Create a date range query on updatedAt. Order by updatedAt ascending.
     */
    @Cacheable(cacheNames = "delivery-service::S4-F6", key = "#orderId + ':' + #startDate + ':' + #endDate")
    public List<Delivery> getOrderDeliveryHistory(
            Long orderId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateOrder(orderId);
        validateDates(startDate, endDate);

        LocalDateTime start = startDate == null
                ? null
                : startDate.atStartOfDay();

        LocalDateTime end = endDate == null
                ? null
                : endDate.atTime(LocalTime.MAX);

        return deliveryRepository.findOrderDeliveryHistory(
                orderId,
                start,
                end
        );
    }

    /**
     * [S4-F3] Find Nearby Deliveries (DTO with Distance)
     * Endpoint: GET /api/deliveries/nearby?lat={lat}&lon={lon}&radiusKm={r}
     * Find active deliveries (status IN ASSIGNED, PICKED_UP, IN_TRANSIT).
     * Calculate distance (Euclidean distance * 111), filter by radius, sort ascending by distance.
     * Response DTO: deliveryId, driverName, orderId, latitude, longitude, distanceKm.
     */
    @Cacheable(cacheNames = "delivery-service::S4-F3", key = "#lat + ',' + #lon + ',' + #radiusKm")
    public List<NearbyDeliveryDTO> getNearbyDeliveries(
            Double lat,
            Double lon,
            Double radiusKm
    ) {
        return deliveryRepository.findNearbyDeliveries(lat, lon, radiusKm)
                .stream()
                .map(row -> NearbyDeliveryDTO.builder()
                        .deliveryId(((Number) row[0]).longValue())
                        .driverName((String) row[1])
                        .orderId(((Number) row[2]).longValue())
                        .latitude(((Number) row[3]).doubleValue())
                        .longitude(((Number) row[4]).doubleValue())
                        .distanceKm(((Number) row[5]).doubleValue())
                        .build()
                )
                .toList();
    }

    /**
     * [S4-F9] Find Delayed Deliveries (DTO with Estimated Arrival)
     * Endpoint: GET /api/deliveries/delayed?maxEstimatedArrival={t}&sinceMinutes={m}
     * Find deliveries with estimated arrival time (calculated in query
     * [CRUD Read] Get Delayed Deliveries (Performance Query)
     * DTO-returning with Object[] Adapter pattern
     */
    @Cacheable(cacheNames = "delivery-service::S4-F9", key = "#maxEstimatedArrival + ':' + #sinceMinutes")
    public List<DelayedDeliveryDTO> getDelayedDeliveries(
            Double maxEstimatedArrival,
            int sinceMinutes
    ) {
        return deliveryRepository.findDelayedDeliveries(maxEstimatedArrival, sinceMinutes)
                .stream()
                .map(row -> DelayedDeliveryDTO.builder()
                        .deliveryId(((Number) row[0]).longValue())
                        .driverName((String) row[1])
                        .orderId(((Number) row[2]).longValue())
                        .latitude(((Number) row[3]).doubleValue())
                        .longitude(((Number) row[4]).doubleValue())
                        .estimatedArrival(((Number) row[5]).doubleValue())
                        .updatedAt((LocalDateTime) row[6])
                        .build()
                )
                .toList();
    }

    /**
     * [S4-F7] Purge Old Delivery Records (Transactional)
     * Endpoint: DELETE /api/deliveries/purge?olderThanDays={n}
     * Logs: OLD_DATA_PURGED event with deletion count and cutoff date
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::delivery", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F5", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true), @CacheEvict(cacheNames = "delivery-service::S4-F10", allEntries = true)
    })
    public Map<String, Integer> purgeOldDeliveries(Integer olderThanDays) {
        if (olderThanDays == null || olderThanDays <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "olderThanDays must be greater than 0");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        String deliveredStatus = DeliveryStatus.DELIVERED.name();

        long count = deliveryRepository.countOldByStatus(deliveredStatus, cutoff);
        int deletedCount = 0;
        if (count > 0) {
            deletedCount = deliveryRepository.deleteOldByStatus(deliveredStatus, cutoff);
        }

        // [S4-F7] OLD_DATA_PURGED: Notify observers to log event
        if (deletedCount > 0) {
            Map<String, Object> eventDetails = new HashMap<>();
            eventDetails.put("olderThanDays", olderThanDays);
            eventDetails.put("cutoffDate", cutoff);
            eventDetails.put("status", deliveredStatus);
            eventDetails.put("deletedCount", deletedCount);

            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("deliveryId", 0L); // System-wide purge; use 0 as a placeholder
            eventPayload.put("details", eventDetails);

            notifyObservers("OLD_DATA_PURGED", eventPayload);
        }

        Map<String, Integer> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        return response;
    }

    /**
     * [S4-F8] Delivery Performance Summary (DTO)
     * Endpoint: GET /api/deliveries/driver/{driverName}/summary?startDate={d}&endDate={d}
     * [CRUD Read] Get Delivery Performance Summary (Report DTO)
     * DTO-returning with Builder pattern
     */
    @Cacheable(cacheNames = "delivery-service::S4-F8", key = "#driverName + ':' + #startDate + ':' + #endDate")
    @Transactional(readOnly = true)
    public DeliveryPerformanceSummaryDTO getDeliveryPerformanceSummary(
            String driverName,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDates(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> results =
                deliveryRepository.findPerformanceSummary(driverName, start, end);

        if (results.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No deliveries found for driver: " + driverName
            );
        }

        Object[] row = results.getFirst();

        return DeliveryPerformanceSummaryDTO.builder()
                .driverName((String) row[0])
                .totalDeliveries(((Number) row[1]).longValue())
                .averageSpeed(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                .maxSpeed(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .firstDelivery((LocalDateTime) row[4])
                .lastDelivery((LocalDateTime) row[5])
                .build();
    }

    public DeliveryAnalyticsDTO getDeliveryAnalytics(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDates(startDate, endDate);

        // always run event logging even if cache hit
        Map<String, Object> details = new HashMap<>();
        details.put("startDate", startDate);
        details.put("endDate", endDate);

        Map<String, Object> payload = new HashMap<>();
        payload.put("details", details);

        notifyObservers("ANALYTICS_VIEWED", payload);

        String key = startDate + ":" + endDate;

        Cache cache = cacheManager.getCache("delivery-service::S4-F10");

        if (cache != null) {
            DeliveryAnalyticsDTO cached =
                    cache.get(key, DeliveryAnalyticsDTO.class);

            if (cached != null) {
                return cached;
            }
        }

        DeliveryAnalyticsDTO dto =
                buildDeliveryAnalytics(startDate, endDate);

        if (cache != null) {
            cache.put(key, dto);
        }

        return dto;
    }
    @Cacheable(cacheNames = "delivery-service::S4-F12", key = "#deliveryId + ':' + #startTime + ':' + #endTime")
    @Transactional(readOnly = true)
    public List<DeliveryTrackingDTO> getDeliveryTrackingTimeline(Long deliveryId, String startTime, String endTime) {
        getDeliveryById(deliveryId);

        List<DeliveryTrackingEvent> events;
        if (startTime != null && endTime != null) {
            try {
                events = trackingEventRepository.findByKeyDeliveryIdInTimeRange(
                        deliveryId, parseTimeParam(startTime), parseTimeParam(endTime));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid time format — use HH:mm (e.g. 14:10) or ISO-8601 (e.g. 2024-01-01T14:00:00Z)");
            }
        } else {
            events = trackingEventRepository.findByKeyDeliveryIdOrderByKeyTimestampDesc(deliveryId);
        }

        return events.stream().map(cassandraRowAdapter::adapt).toList();
    }

    private DeliveryAnalyticsDTO buildDeliveryAnalytics(
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDateTime start = startDate == null
                ? null
                : startDate.atStartOfDay();

        LocalDateTime end = endDate == null
                ? null
                : endDate.atTime(23, 59, 59, 999_000_000);

        long total =
                deliveryRepository.countTotalDeliveries(start, end);

        Double avg =
                deliveryRepository.averageDeliveryMinutes(start, end);

        Long delivered =
                deliveryRepository.countDeliveredOrders(start, end);

        Long onTime =
                deliveryRepository.countOnTimeDeliveredOrders(start, end);

        Map<DeliveryStatus, Long> grouped =
                deliveryRepository.countByStatus(start, end)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> DeliveryStatus.valueOf(row[0].toString()),
                                row -> ((Number) row[1]).longValue()
                        ));

        double average = avg == null ? 0.0 : avg;

        double rate =
                delivered == null || delivered == 0
                        ? 0.0
                        : (double) onTime / delivered;

        return DeliveryAnalyticsDTO.builder()
                .totalDeliveries(total)
                .averageDeliveryTimeMinutes(average)
                .onTimeRate(rate)
                .deliveriesByStatus(grouped)
                .build();
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null &&
                startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range"
            );
        }
    }
}

