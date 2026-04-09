package com.team05.fooddelivery.delivery.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDateTime;

import com.team05.fooddelivery.delivery.dto.DelayedDeliveryDTO;
import com.team05.fooddelivery.delivery.dto.NearbyDeliveryDTO;
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

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

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

    @Transactional(readOnly = true)
    public Delivery getLatestDeliveryByOrderId(Long orderId) {
validateOrder(orderId);

        return deliveryRepository.findLatestByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

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

    public void deleteDelivery(Long id) {
        if (!deliveryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found");
        }
        deliveryRepository.deleteById(id);
    }

    private void validateOrder(Long orderId) {
        if (!deliveryRepository.orderExists(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

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

    public List<NearbyDeliveryDTO> getNearbyDeliveries(Double lat, Double lon, Double radiusKm) {
        return deliveryRepository.findNearbyDeliveries(lat, lon, radiusKm)
                .stream()
                .map(row -> new NearbyDeliveryDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).doubleValue(),
                        ((Number) row[5]).doubleValue()
                ))
                .toList();
    }

    public List<DelayedDeliveryDTO> getDelayedDeliveries(Double maxEstimatedArrival, int sinceMinutes) {
        return deliveryRepository.findDelayedDeliveries(maxEstimatedArrival, sinceMinutes)
                .stream()
                .map(row -> new DelayedDeliveryDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).doubleValue(),
                        ((Number) row[5]).doubleValue(),
                        (LocalDateTime) row[6]
                ))
                .toList();
    }

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
}

