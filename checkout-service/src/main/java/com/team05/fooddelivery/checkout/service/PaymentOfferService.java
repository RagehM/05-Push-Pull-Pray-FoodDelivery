package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.dto.OfferUsageDTO;
import com.team05.fooddelivery.checkout.enums.OfferDiscountType;
import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.repository.PaymentOfferRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.team05.fooddelivery.checkout.dto.PaymentOfferDTO;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.model.Offer;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentOfferService {
    private final PaymentOfferRepository paymentOfferRepository;
    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;

    public PaymentOfferService(PaymentOfferRepository paymentOfferRepository, PaymentRepository paymentRepository, OfferRepository offerRepository) {
        this.paymentOfferRepository = paymentOfferRepository;
        this.paymentRepository = paymentRepository;
        this.offerRepository = offerRepository;
    }

    public PaymentOffer createPaymentOffer(PaymentOfferDTO dto) {
        PaymentOffer paymentOffer = new PaymentOffer();


        Payment payment = null;
        if (dto.paymentId() != null) {
            payment = paymentRepository.findById(dto.paymentId()).orElse(null);
        }
        paymentOffer.setPayment(payment);

        Offer offer = null;
        if (dto.offerId() != null) {
            offer = offerRepository.findById(dto.offerId()).orElse(null);
        }
        paymentOffer.setOffer(offer);

        paymentOffer.setDiscountApplied(dto.discountApplied() != null ? dto.discountApplied() : 0.0);

        return paymentOfferRepository.save(paymentOffer);
    }

    public List<PaymentOffer> getPaymentOffers() {
        return paymentOfferRepository.findAll();
    }

    @Cacheable(value = "checkout-service::payment-offer", key = "#paymentOfferId")
    public PaymentOffer getPaymentOfferById(Long paymentOfferId) {
        return paymentOfferRepository.findById(paymentOfferId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PaymentOffer not found"));
    }

    @Caching(
            put = @CachePut(value = "checkout-service::payment-offer", key = "#paymentOfferId"),
            evict = @CacheEvict(value = "checkout-service::S5-F9", allEntries = true)
    )
    public PaymentOffer updatePaymentOffer(Long paymentOfferId, PaymentOfferDTO dto) {
        return paymentOfferRepository.findById(paymentOfferId).map(paymentOffer -> {
            if (dto.discountApplied() != null)
                paymentOffer.setDiscountApplied(dto.discountApplied());

            if (dto.paymentId() != null) {
                Payment payment = paymentRepository.findById(dto.paymentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
                paymentOffer.setPayment(payment);
            }

            if (dto.offerId() != null) {
                Offer offer = offerRepository.findById(dto.offerId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
                paymentOffer.setOffer(offer);
            }

            return paymentOfferRepository.save(paymentOffer);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PaymentOffer not found"));
    }

    @Caching(evict = {
            @CacheEvict(value = "checkout-service::payment-offer", key = "#paymentOfferId"),
            @CacheEvict(value = "checkout-service::S5-F9", allEntries = true)
    })
    public void deletePaymentOfferById(Long paymentOfferId) {
        paymentOfferRepository.deleteById(paymentOfferId);
    }

    // [S5-F9] Get Most Used Offers Report (Join Entity + Aggregation)
    @Cacheable(value = "checkout-service::S5-F9", key = "#limit")
    public List<OfferUsageDTO> getMostUsedOffers(int limit) {
        List<Object[]> rows = offerRepository.findMostUsedOffers(PageRequest.of(0, limit));

        return rows.stream().map(row -> {
            Long offerId = ((Number) row[0]).longValue();
            String code = (String) row[1];
            OfferDiscountType discountType = OfferDiscountType.valueOf((String) row[2]);
            Double discountValue = ((Number) row[3]).doubleValue();
            Integer timesUsed = ((Number) row[4]).intValue();
            Double totalDiscountGiven = ((Number) row[5]).doubleValue();
            Boolean active = (Boolean) row[6];
            Object rawDate = row[7];
            LocalDateTime expiryDate = rawDate instanceof java.sql.Timestamp ts
                    ? ts.toLocalDateTime()
                    : (LocalDateTime) rawDate;
            Boolean expired = expiryDate.isBefore(LocalDateTime.now());

            return OfferUsageDTO.builder()
                    .offerId(offerId)
                    .code(code)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .timesUsed(timesUsed)
                    .totalDiscountGiven(totalDiscountGiven)
                    .active(active)
                    .expired(expired)
                    .build();
        }).toList();
    }
}
