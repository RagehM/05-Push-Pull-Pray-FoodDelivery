package com.team05.fooddelivery.delivery.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.team05.fooddelivery.contracts.dto.DeliveryDTO;
import com.team05.fooddelivery.delivery.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team05.fooddelivery.delivery.model.Delivery;
import com.team05.fooddelivery.delivery.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public ResponseEntity<Delivery> createDelivery(@RequestBody Delivery delivery) {
        log.info("Received {} {}", "POST", "/api/deliveries");
        Delivery result = deliveryService.createDelivery(delivery);
        log.info("Returning {} for {} {}", HttpStatus.CREATED, "POST", "/api/deliveries");
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<Delivery> createOrderDelivery(@PathVariable Long orderId, @RequestBody Delivery delivery) {
        log.info("Received {} {}", "POST", "/api/deliveries/order/" + orderId);
        Delivery result = deliveryService.createOrderDelivery(orderId, delivery);
        log.info("Returning {} for {} {}", HttpStatus.CREATED, "POST", "/api/deliveries/order/" + orderId);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/tracking")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> recordDeliveryTracking(@PathVariable Long id,
                                                       @RequestBody DeliveryTrackingRequestDTO request) {
        log.info("Received {} {}", "POST", "/api/deliveries/" + id + "/tracking");
        deliveryService.recordDeliveryTracking(id, request);
        log.info("Returning {} for {} {}", HttpStatus.CREATED, "POST", "/api/deliveries/" + id + "/tracking");
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public Delivery getDeliveryById(@PathVariable Long id) {
        log.info("Received {} {}", "GET", "/api/deliveries/" + id);
        Delivery result = deliveryService.getDeliveryById(id);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/" + id);
        return result;
    }

    @GetMapping("/order/{orderId}/history")
    public List<Delivery> getOrderDeliveryHistory(
            @PathVariable Long orderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Received {} {}", "GET", "/api/deliveries/order/" + orderId + "/history");
        List<Delivery> result = deliveryService.getOrderDeliveryHistory(orderId, startDate, endDate);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/order/" + orderId + "/history");
        return result;
    }

    @GetMapping
    public List<Delivery> getAllDeliveries(@RequestParam(required = false) String status) {
        log.info("Received {} {}", "GET", "/api/deliveries");
        List<Delivery> result = deliveryService.getAllDeliveries(status);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries");
        return result;
    }

    @GetMapping("/order/{orderId}/latest")
    public Delivery getLatestDeliveryByOrderId(@PathVariable Long orderId) {
        log.info("Received {} {}", "GET", "/api/deliveries/order/" + orderId + "/latest");
        Delivery result = deliveryService.getLatestDeliveryByOrderId(orderId);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/order/" + orderId + "/latest");
        return result;
    }

    @GetMapping("/metadata/search")
    public List<Delivery> searchDeliveriesByMetadata(@RequestParam String key,
                                                     @RequestParam String operator,
                                                     @RequestParam String value) {
        log.info("Received {} {}", "GET", "/api/deliveries/metadata/search");
        List<Delivery> result = deliveryService.searchDeliveriesByMetadata(key, operator, value);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/metadata/search");
        return result;
    }

    @GetMapping("/nearby")
    public List<NearbyDeliveryDTO> getNearbyDeliveries(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radiusKm) {
        log.info("Received {} {}", "GET", "/api/deliveries/nearby");
        List<NearbyDeliveryDTO> result = deliveryService.getNearbyDeliveries(lat, lon, radiusKm);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/nearby");
        return result;
    }

    @PutMapping("/{id}")
    public Delivery updateDelivery(@PathVariable Long id, @RequestBody Delivery delivery) {
        log.info("Received {} {}", "PUT", "/api/deliveries/" + id);
        Delivery result = deliveryService.updateDelivery(id, delivery);
        log.info("Returning {} for {} {}", HttpStatus.OK, "PUT", "/api/deliveries/" + id);
        return result;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable Long id) {
        log.info("Received {} {}", "DELETE", "/api/deliveries/" + id);
        deliveryService.deleteDelivery(id);
        log.info("Returning {} for {} {}", HttpStatus.NO_CONTENT, "DELETE", "/api/deliveries/" + id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Integer> batchCreate(@RequestBody BatchDeliveryRequestDTO request) {
        log.info("Received {} {}", "POST", "/api/deliveries/batch");
        int count = deliveryService.batchCreate(request);
        log.info("Returning {} for {} {}", HttpStatus.CREATED, "POST", "/api/deliveries/batch");
        return new ResponseEntity<>(count, HttpStatus.CREATED);
    }

    @GetMapping("/delayed")
    public List<DelayedDeliveryDTO> getDelayedDeliveries(
            @RequestParam Double maxEstimatedArrival,
            @RequestParam int sinceMinutes) {
        log.info("Received {} {}", "GET", "/api/deliveries/delayed");
        List<DelayedDeliveryDTO> result = deliveryService.getDelayedDeliveries(maxEstimatedArrival, sinceMinutes);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/delayed");
        return result;
    }

    @DeleteMapping("/purge")
    public Map<String, Integer> purgeOldDeliveries(@RequestParam Integer olderThanDays) {
        log.info("Received {} {}", "DELETE", "/api/deliveries/purge");
        Map<String, Integer> result = deliveryService.purgeOldDeliveries(olderThanDays);
        log.info("Returning {} for {} {}", HttpStatus.OK, "DELETE", "/api/deliveries/purge");
        return result;
    }

    @GetMapping("/{id}/tracking")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public List<DeliveryTrackingDTO> getDeliveryTrackingTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("Received {} {}", "GET", "/api/deliveries/" + id + "/tracking");
        List<DeliveryTrackingDTO> result = deliveryService.getDeliveryTrackingTimeline(id, startTime, endTime);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/" + id + "/tracking");
        return result;
    }

    @GetMapping("/driver/{driverName}/summary")
    public DeliveryPerformanceDTO getDeliveryPerformanceSummary(
            @PathVariable String driverName,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        log.info("Received {} {}", "GET", "/api/deliveries/driver/" + driverName + "/summary");
        DeliveryPerformanceDTO result = deliveryService.getDeliveryPerformanceSummary(driverName, startDate, endDate);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/driver/" + driverName + "/summary");
        return result;
    }

    @GetMapping("/order/{orderId}/active")
    public DeliveryDTO getActiveDeliveryStatys(@PathVariable Long orderId) {
        log.info("Received {} {}", "GET", "/api/deliveries/order/" + orderId + "/active");
        DeliveryDTO result = deliveryService.getDeliveryActiveStatus(orderId);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/order/" + orderId + "/active");
        return result;
    }

    @GetMapping("/analytics")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public DeliveryAnalyticsDTO getAnalytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        log.info("Received {} {}", "GET", "/api/deliveries/analytics");
        DeliveryAnalyticsDTO result = deliveryService.getDeliveryAnalytics(startDate, endDate);
        log.info("Returning {} for {} {}", HttpStatus.OK, "GET", "/api/deliveries/analytics");
        return result;
    }
}

