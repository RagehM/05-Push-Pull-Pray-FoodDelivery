package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import com.team05.fooddelivery.user.service.DeliveryAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery_addresses")
public class DeliveryAddressController {
    private final DeliveryAddressService deliveryAddressService;

    @Autowired
    public DeliveryAddressController(DeliveryAddressService deliveryAddressService)
    {
        this.deliveryAddressService = deliveryAddressService;
    }

    @PostMapping
    public DeliveryAddress createDeliveryAddress(DeliveryAddress deliveryAddress)
    {
        return deliveryAddressService.createDeliveryAddress(deliveryAddress);
    }

    @GetMapping
    public List<DeliveryAddress> getAllDeliveryAddress()
    {
        return deliveryAddressService.findAll();
    }

    @GetMapping("/{id}")
    public DeliveryAddress getDeliveryAddress(@PathVariable Long id)
    {
        return deliveryAddressService.findById(id);
    }

    @PutMapping("/{id}")
    public DeliveryAddress updateDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress, @PathVariable Long id)
    {
        return deliveryAddressService.updateDeliveryAddress(deliveryAddress, id);
    }

    @DeleteMapping("/{id}")
    public void deleteDeliveryAddress(@PathVariable Long id)
    {
        deliveryAddressService.deleteDeliveryAddress(id);
    }
}
