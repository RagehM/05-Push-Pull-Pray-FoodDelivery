package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryAddressService {
    private final DeliveryAddressRepository deliveryAddressRepository;

    @Autowired
    public DeliveryAddressService(DeliveryAddressRepository deliveryAddressRepository) {
        this.deliveryAddressRepository = deliveryAddressRepository;
    }

    public List<DeliveryAddress> findAll()
    {
        return deliveryAddressRepository.findAll();
    }

    public DeliveryAddress findById(Long id)
    {
        return deliveryAddressRepository.findById(id).get();
    }

    public DeliveryAddress updateDeliveryAddress(DeliveryAddress deliveryAddress, Long id)
    {
        DeliveryAddress updatedDeliveryAddress = deliveryAddressRepository.findById(id).get();
        updatedDeliveryAddress.setCity(deliveryAddress.getCity() == null? updatedDeliveryAddress.getCity() : deliveryAddress.getCity());
        updatedDeliveryAddress.setLatitude(deliveryAddress.getLatitude() == null? updatedDeliveryAddress.getLatitude() : deliveryAddress.getLatitude());
        updatedDeliveryAddress.setLongitude(deliveryAddress.getLongitude() == null? updatedDeliveryAddress.getLongitude() : deliveryAddress.getLongitude());
        updatedDeliveryAddress.setStreetAddress(deliveryAddress.getStreetAddress() == null? updatedDeliveryAddress.getStreetAddress() : deliveryAddress.getStreetAddress());
        updatedDeliveryAddress.setLabel(deliveryAddress.getLabel() == null? updatedDeliveryAddress.getLabel() : deliveryAddress.getLabel());
        updatedDeliveryAddress.setDefault(deliveryAddress.getDefault() == null? updatedDeliveryAddress.getDefault() : deliveryAddress.getDefault());
        updatedDeliveryAddress.setUser(deliveryAddress.getUser() == null? updatedDeliveryAddress.getUser() : deliveryAddress.getUser());
        updatedDeliveryAddress.setMetadata(deliveryAddress.getMetadata() == null? updatedDeliveryAddress.getMetadata() : deliveryAddress.getMetadata());
        return deliveryAddressRepository.save(updatedDeliveryAddress);
    }

    public DeliveryAddress createDeliveryAddress(DeliveryAddress deliveryAddress)
    {
        if (deliveryAddress.getCreatedAt() == null || deliveryAddress.getCreatedAt().equals("") || deliveryAddress.getCreatedAt().equals("null"))
        {
            deliveryAddress.setCreatedAt(LocalDateTime.now());
        }
        return deliveryAddressRepository.save(deliveryAddress);
    }

    public void deleteDeliveryAddress(Long id)
    {
        deliveryAddressRepository.deleteById(id);
    }
}
