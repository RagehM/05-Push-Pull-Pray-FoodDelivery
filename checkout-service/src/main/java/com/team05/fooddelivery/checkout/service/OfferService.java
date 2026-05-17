
package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.model.Offer;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OfferService {
    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    // Offer CRUD
    public Offer createOffer(Offer offer) {
        return offerRepository.save(offer);
    }

    public List<Offer> getOffers() {
        return offerRepository.findAll();
    }

    @Cacheable(value = "checkout-service::offer", key = "#id")
    public Offer getOfferById(long id) {
        return offerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    }

    @Caching(
            put = @CachePut(value = "checkout-service::offer", key = "#id"),
            evict = {
                    @CacheEvict(value = "checkout-service::S5-F8", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F9", allEntries = true)
            }
    )
    public Offer updateOffer(Long id, Offer updatedOffer) {
        return offerRepository.findById(id).map(offer -> {
            offer.setCode(updatedOffer.getCode());
            offer.setDiscountType(updatedOffer.getDiscountType());
            offer.setDiscountValue(updatedOffer.getDiscountValue());
            offer.setMaxUses(updatedOffer.getMaxUses());
            offer.setCurrentUses(updatedOffer.getCurrentUses());
            offer.setExpiryDate(updatedOffer.getExpiryDate());
            offer.setActive(updatedOffer.getActive());
            offer.setMetadata(updatedOffer.getMetadata());
            offer.setPaymentOffers(updatedOffer.getPaymentOffers());
            return offerRepository.save(offer);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    }

    @Caching(evict = {
            @CacheEvict(value = "checkout-service::offer", key = "#id"),
            @CacheEvict(value = "checkout-service::S5-F8", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F9", allEntries = true)
    })
    public void deleteOfferById(long id) {
        if (!offerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found");
        }
        offerRepository.deleteById(id);
    }

}


