package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.model.DeliveryAddress;
import com.team05.fooddelivery.user.repository.DeliveryAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        updatedDeliveryAddress.setCity(deliveryAddress.getCity());
        updatedDeliveryAddress.setLatitude(deliveryAddress.getLatitude());
        updatedDeliveryAddress.setLongitude(deliveryAddress.getLongitude());
        updatedDeliveryAddress.setStreetAddress(deliveryAddress.getStreetAddress());
        updatedDeliveryAddress.setLabel(deliveryAddress.getLabel());
        return deliveryAddressRepository.save(updatedDeliveryAddress);
    }

    public DeliveryAddress createDeliveryAddress(DeliveryAddress deliveryAddress)
    {
        return deliveryAddressRepository.save(deliveryAddress);
    }

    public void deleteDeliveryAddress(Long id)
    {
        deliveryAddressRepository.deleteById(id);
    }
}
