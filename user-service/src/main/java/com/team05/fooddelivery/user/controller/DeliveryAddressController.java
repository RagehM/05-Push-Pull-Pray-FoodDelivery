package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import com.team05.fooddelivery.user.service.DeliveryAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class DeliveryAddressController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAddressController.class);
    private final DeliveryAddressService deliveryAddressService;

    @Autowired
    public DeliveryAddressController(DeliveryAddressService deliveryAddressService) {
        this.deliveryAddressService = deliveryAddressService;
    }

    @PostMapping
    public DeliveryAddress createDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress) {
        log.info("Received {} {}", "POST", "/api/addresses");
        DeliveryAddress result = deliveryAddressService.createDeliveryAddress(deliveryAddress);
        log.info("Returning {} for {} {}", 200, "POST", "/api/addresses");
        return result;
    }

    @GetMapping
    public List<DeliveryAddress> getAllDeliveryAddress() {
        log.info("Received {} {}", "GET", "/api/addresses");
        List<DeliveryAddress> result = deliveryAddressService.findAll();
        log.info("Returning {} for {} {}", 200, "GET", "/api/addresses");
        return result;
    }

    @GetMapping("/{id}")
    public DeliveryAddress getDeliveryAddress(@PathVariable Long id) {
        log.info("Received {} {}", "GET", "/api/addresses/" + id);
        DeliveryAddress result = deliveryAddressService.findById(id);
        log.info("Returning {} for {} {}", 200, "GET", "/api/addresses/" + id);
        return result;
    }

    @PutMapping("/{id}")
    public DeliveryAddress updateDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress, @PathVariable Long id) {
        log.info("Received {} {}", "PUT", "/api/addresses/" + id);
        DeliveryAddress result = deliveryAddressService.updateDeliveryAddress(deliveryAddress, id);
        log.info("Returning {} for {} {}", 200, "PUT", "/api/addresses/" + id);
        return result;
    }

    @DeleteMapping("/{id}")
    public void deleteDeliveryAddress(@PathVariable Long id) {
        log.info("Received {} {}", "DELETE", "/api/addresses/" + id);
        deliveryAddressService.deleteDeliveryAddress(id);
        log.info("Returning {} for {} {}", 204, "DELETE", "/api/addresses/" + id);
    }
}