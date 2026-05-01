package com.team05.fooddelivery.delivery.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.team05.fooddelivery.delivery.adapter.CassandraRowAdapter;
import com.team05.fooddelivery.delivery.dto.*;
import com.team05.fooddelivery.delivery.model.cassandra.DeliveryTrackingEvent;
import com.team05.fooddelivery.delivery.repository.cassandra.DeliveryTrackingEventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.team05.fooddelivery.delivery.enums.DeliveryStatus;
import com.team05.fooddelivery.delivery.model.Delivery;
import com.team05.fooddelivery.delivery.repository.DeliveryRepository;

@Service
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryTrackingEventRepository trackingEventRepository;
    private final CassandraRowAdapter cassandraRowAdapter = new CassandraRowAdapter();

    public DeliveryService(DeliveryRepository deliveryRepository,
                           DeliveryTrackingEventRepository trackingEventRepository) {
        this.deliveryRepository = deliveryRepository;
        this.trackingEventRepository = trackingEventRepository;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true)
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
        return deliveryRepository.save(delivery);
    }

    public Delivery createDelivery(Delivery delivery) {
        if (delivery.getMetadata() == null) {
            delivery.setMetadata(new HashMap<>());
        }
        if (delivery.getStatus() == null) {
            delivery.setStatus(DeliveryStatus.ASSIGNED);
        }
        return deliveryRepository.save(delivery);
    }

    @Cacheable(cacheNames = "delivery-service::delivery", key = "#id")
    @Transactional(readOnly = true)
    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

    @Transactional(readOnly = true)
    public List<Delivery> getAllDeliveries(String status) {
        if (status == null || status.isBlank()) {
            return deliveryRepository.findAll();
        }
        return deliveryRepository.findByStatus(status);
    }

    @Cacheable(cacheNames = "delivery-service::S4-F1", key = "#orderId")
    @Transactional(readOnly = true)
    public Delivery getLatestDeliveryByOrderId(Long orderId) {
validateOrder(orderId);

        return deliveryRepository.findLatestByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

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

    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::delivery", key = "#id"),
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F5", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true)
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

        return deliveryRepository.save(existingDelivery);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::delivery", key = "#id"),
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F5", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true)
    })
    public void deleteDelivery(Long id) {
        if (!deliveryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found");
        }
        deliveryRepository.deleteById(id);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true)
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
        return toSave.size();
    }

    private void validateOrder(Long orderId) {
        if (!deliveryRepository.orderExists(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    @Cacheable(cacheNames = "delivery-service::S4-F6", key = "#orderId + ':' + #startDate + ':' + #endDate")
    public List<Delivery> getOrderDeliveryHistory(Long orderId, LocalDate startDate, LocalDate endDate) {
        validateOrder(orderId);

        // No Date filter
        if (startDate == null && endDate == null) {
            return deliveryRepository.findByOrderIdOrderByUpdatedAtAsc(orderId);
        }

        // Only start date filter
        if (startDate != null && endDate == null) {
            LocalDateTime start = startDate.atStartOfDay(); // 00:00:00 of the start day
            return deliveryRepository
                    .findByOrderIdAndUpdatedAtAfterOrderByUpdatedAtAsc(orderId, start);
        }

        // Only end date filter
        if (startDate == null) {
            LocalDateTime end = endDate.atTime(LocalTime.MAX); // 23:59:59 of the end day
            return deliveryRepository
                    .findByOrderIdAndUpdatedAtBeforeOrderByUpdatedAtAsc(orderId, end);
        }

        LocalDateTime start = startDate.atStartOfDay(); // 00:00:00 of the start day
        LocalDateTime end = endDate.atTime(LocalTime.MAX); // 23:59:59 of the end day

        return deliveryRepository
                .findByOrderIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(orderId, start, end);
    }

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

    @Caching(evict = {
        @CacheEvict(cacheNames = "delivery-service::delivery", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F1", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F3", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F5", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F6", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F8", allEntries = true),
        @CacheEvict(cacheNames = "delivery-service::S4-F9", allEntries = true)
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

        Map<String, Integer> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        return response;
    }

    @Cacheable(cacheNames = "delivery-service::S4-F8", key = "#driverName + ':' + #startDate + ':' + #endDate")
    @Transactional(readOnly = true)
    public DeliveryPerformanceSummaryDTO getDeliveryPerformanceSummary(
            String driverName,
            LocalDate startDate,
            LocalDate endDate
    ) {
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

    @Cacheable(cacheNames = "delivery-service::S4-F12", key = "#deliveryId + ':' + #startTime + ':' + #endTime")
    @Transactional(readOnly = true)
    public List<DeliveryTrackingDTO> getDeliveryTrackingTimeline(Long deliveryId, String startTime, String endTime) {
        getDeliveryById(deliveryId);

        List<DeliveryTrackingEvent> events;
        if (startTime != null && endTime != null) {
            events = trackingEventRepository.findByKeyDeliveryIdAndKeyTimestampBetween(
                    deliveryId, Instant.parse(startTime), Instant.parse(endTime));
        } else {
            events = trackingEventRepository.findByKeyDeliveryId(deliveryId);
        }

        return events.stream().map(cassandraRowAdapter::adapt).toList();
    }

}

