package com.team05.fooddelivery.delivery.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.team05.fooddelivery.delivery.dto.NearbyDeliveryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public ResponseEntity<Delivery> createDelivery(@RequestBody Delivery delivery) {
        return new ResponseEntity<>(deliveryService.createDelivery(delivery), HttpStatus.CREATED);
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<Delivery> createOrderDelivery(@PathVariable Long orderId, @RequestBody Delivery delivery) {
        return new ResponseEntity<>(deliveryService.createOrderDelivery(orderId, delivery), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public Delivery getDeliveryById(@PathVariable Long id) {
        return deliveryService.getDeliveryById(id);
    }

    @GetMapping("/order/{orderId}/history")
    public List<Delivery> getOrderDeliveryHistory(
            @PathVariable Long orderId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        return deliveryService.getOrderDeliveryHistory(orderId, startDate, endDate);
    }

    @GetMapping
    public List<Delivery> getAllDeliveries(@RequestParam(required = false) String status) {
        return deliveryService.getAllDeliveries(status);
    }

    @GetMapping("/order/{orderId}/latest")
    public Delivery getLatestDeliveryByOrderId(@PathVariable Long orderId) {
        return deliveryService.getLatestDeliveryByOrderId(orderId);
    }

    @GetMapping("/metadata/search")
    public List<Delivery> searchDeliveriesByMetadata(@RequestParam String key,
                                                     @RequestParam String operator,
                                                     @RequestParam String value) {
        return deliveryService.searchDeliveriesByMetadata(key, operator, value);
    }

    @GetMapping("/nearby")
    public List<NearbyDeliveryDTO> getNearbyDeliveries(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radiusKm) {

        return deliveryService.getNearbyDeliveries(lat, lon, radiusKm);
    }

    @PutMapping("/{id}")
    public Delivery updateDelivery(@PathVariable Long id, @RequestBody Delivery delivery) {
        return deliveryService.updateDelivery(id, delivery);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }
}

