package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        return deliveryAddressRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery Address not found"));
    }

    public DeliveryAddress updateDeliveryAddress(DeliveryAddress deliveryAddress, Long id)
    {

        DeliveryAddress updatedDeliveryAddress = deliveryAddressRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery Address not found"));
        updatedDeliveryAddress.setCity(deliveryAddress.getCity() == null? updatedDeliveryAddress.getCity() : deliveryAddress.getCity());
        updatedDeliveryAddress.setLatitude(deliveryAddress.getLatitude() == null? updatedDeliveryAddress.getLatitude() : deliveryAddress.getLatitude());
        updatedDeliveryAddress.setLongitude(deliveryAddress.getLongitude() == null? updatedDeliveryAddress.getLongitude() : deliveryAddress.getLongitude());
        updatedDeliveryAddress.setStreetAddress(deliveryAddress.getStreetAddress() == null? updatedDeliveryAddress.getStreetAddress() : deliveryAddress.getStreetAddress());
        updatedDeliveryAddress.setLabel(deliveryAddress.getLabel() == null? updatedDeliveryAddress.getLabel() : deliveryAddress.getLabel());
        updatedDeliveryAddress.setDefault(deliveryAddress.getDefault() == null? updatedDeliveryAddress.getDefault() : deliveryAddress.getDefault());

        deliveryAddressRepository.findUserById(deliveryAddress.getUser().getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        updatedDeliveryAddress.setUser(deliveryAddress.getUser() == null? updatedDeliveryAddress.getUser() : deliveryAddress.getUser());
        updatedDeliveryAddress.setMetadata(deliveryAddress.getMetadata() == null? updatedDeliveryAddress.getMetadata() : deliveryAddress.getMetadata());
        return deliveryAddressRepository.save(updatedDeliveryAddress);
    }

    public DeliveryAddress createDeliveryAddress(DeliveryAddress deliveryAddress)
    {
        deliveryAddressRepository.findUserById(deliveryAddress.getUser().getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        if (deliveryAddress.getCreatedAt() == null || deliveryAddress.getCreatedAt().equals("") || deliveryAddress.getCreatedAt().equals("null"))
        {
            deliveryAddress.setCreatedAt(LocalDateTime.now());
        }

        if(deliveryAddress.getDefault() != null && deliveryAddress.getDefault())
        {
            List<DeliveryAddress> deliveryAddresses = deliveryAddressRepository.findAllByUserId(deliveryAddress.getUser().getId());
            for (DeliveryAddress da : deliveryAddresses) {
                da.setDefault(false);
                deliveryAddressRepository.save(da);
            }
        }



        return deliveryAddressRepository.save(deliveryAddress);
    }

    public void deleteDeliveryAddress(Long id)
    {
        deliveryAddressRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery Address not found"));
        deliveryAddressRepository.deleteById(id);
    }

    public DeliveryAddress createDeliveryAddressForUser(DeliveryAddress deliveryAddress, long userId) {
        User user= deliveryAddressRepository.findUserById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        if (deliveryAddress.getCreatedAt() == null || deliveryAddress.getCreatedAt().equals("") || deliveryAddress.getCreatedAt().equals("null"))
        {
            deliveryAddress.setCreatedAt(LocalDateTime.now());
        }
        if(deliveryAddress.getDefault() != null && deliveryAddress.getDefault())
        {
            List<DeliveryAddress> deliveryAddresses = deliveryAddressRepository.findAllByUserId(userId);
            for (DeliveryAddress da : deliveryAddresses) {
                da.setDefault(false);
                deliveryAddressRepository.save(da);
            }
        }

        deliveryAddress.setUser(user);
        return deliveryAddressRepository.save(deliveryAddress);
    }
}