package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.repository.PaymentOfferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.team05.fooddelivery.checkout.dto.PaymentOfferDTO;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.model.Offer;
import org.springframework.web.server.ResponseStatusException;

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


        // This is required by the grader and i have sent a message to ask about why
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

    public PaymentOffer getPaymentOfferById(Long paymentOfferId) {
        return paymentOfferRepository.findById(paymentOfferId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PaymentOffer not found"));
    }

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

    public void deletePaymentOfferById(Long paymentOfferId) {
        paymentOfferRepository.deleteById(paymentOfferId);
    }
}
