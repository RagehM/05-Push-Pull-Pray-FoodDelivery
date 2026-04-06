package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.repository.PaymentOfferRepository;
import org.springframework.stereotype.Service;
import com.team05.fooddelivery.checkout.dto.PaymentOfferDTO;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.model.Offer;

import java.util.List;

@Service
public class PaymentOfferService {
    private final PaymentOfferRepository paymentOfferRepository;
    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;

    public PaymentOfferService(PaymentOfferRepository paymentOfferRepository,
                               PaymentRepository paymentRepository,
                               OfferRepository offerRepository) {
        this.paymentOfferRepository = paymentOfferRepository;
        this.paymentRepository = paymentRepository;
        this.offerRepository = offerRepository;
    }

    public PaymentOffer createPaymentOffer(PaymentOfferDTO dto) {
        PaymentOffer paymentOffer = new PaymentOffer();

        paymentOffer.setDiscountApplied(dto.getDiscountApplied());

        Payment payment = paymentRepository.findById(dto.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        paymentOffer.setPayment(payment);

        Offer offer = offerRepository.findById(dto.getOfferId())
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        paymentOffer.setOffer(offer);

        return paymentOfferRepository.save(paymentOffer);
    }

    public List<PaymentOffer> getPaymentOffers() {
        return paymentOfferRepository.findAll();
    }

    public PaymentOffer getPaymentOfferById(Long paymentOfferId) {
        return paymentOfferRepository.findById(paymentOfferId).orElseThrow(() -> new RuntimeException("PaymentOffer not found"));
    }

    public PaymentOffer updatePaymentOffer(Long paymentOfferId, PaymentOfferDTO dto) {
        return paymentOfferRepository.findById(paymentOfferId).map(paymentOffer -> {
            if (dto.getDiscountApplied() != null)
                paymentOffer.setDiscountApplied(dto.getDiscountApplied());

            if (dto.getPaymentId() != null) {
                Payment payment = paymentRepository.findById(dto.getPaymentId())
                        .orElseThrow(() -> new RuntimeException("Payment not found"));
                paymentOffer.setPayment(payment);
            }

            if (dto.getOfferId() != null) {
                Offer offer = offerRepository.findById(dto.getOfferId())
                        .orElseThrow(() -> new RuntimeException("Offer not found"));
                paymentOffer.setOffer(offer);
            }

            return paymentOfferRepository.save(paymentOffer);
        }).orElseThrow(() -> new RuntimeException("PaymentOffer not found"));
    }

    public void deletePaymentOfferById(Long paymentOfferId) {
        paymentOfferRepository.deleteById(paymentOfferId);
    }
}
