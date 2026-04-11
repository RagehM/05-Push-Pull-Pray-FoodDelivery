
package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.dto.OfferUsageDTO;
import com.team05.fooddelivery.checkout.enums.OfferDiscountType;
import com.team05.fooddelivery.checkout.model.Offer;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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

    public Offer getOfferById(long id) {
        return offerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    }

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

    public void deleteOfferById(long id) {
        if (!offerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found");
        }
        offerRepository.deleteById(id);
    }

    // S5-F9: Get Most Used Offers Report
    public List<OfferUsageDTO> getMostUsedOffers(int limit) {
        List<Object[]> rows = offerRepository.findMostUsedOffers(PageRequest.of(0, limit));

        return rows.stream().map(row -> {
            Long offerId        = ((Number) row[0]).longValue();
            String code         = (String) row[1];
            OfferDiscountType discountType = OfferDiscountType.valueOf((String) row[2]);
            Double discountValue    = ((Number) row[3]).doubleValue();
            Integer timesUsed       = ((Number) row[4]).intValue();
            Double totalDiscountGiven = ((Number) row[5]).doubleValue();
            Boolean active          = (Boolean) row[6];

            // PostgreSQL TIMESTAMP comes back as java.sql.Timestamp from native queries
            LocalDateTime expiryDate = ((Timestamp) row[7]).toLocalDateTime();
            Boolean expired = expiryDate.isBefore(LocalDateTime.now());

            return new OfferUsageDTO(
                    offerId, code, discountType, discountValue,
                    timesUsed, totalDiscountGiven, active, expired);
        }).toList();
    }

}


