package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.dto.PaymentOfferDTO;
import com.team05.fooddelivery.checkout.service.PaymentOfferService;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/payment-offers")
public class PaymentOfferController {
    private final PaymentOfferService  paymentOfferService;

    public PaymentOfferController(PaymentOfferService paymentOfferService) {
        this.paymentOfferService = paymentOfferService;
    }

    @PostMapping
    public PaymentOffer createPaymentOffer(@RequestBody PaymentOfferDTO dto) {
        return paymentOfferService.createPaymentOffer(dto);
    }

    @GetMapping
    public List<PaymentOffer> getPaymentOffers() {
        return paymentOfferService.getPaymentOffers();
    }

    @GetMapping("/{id}")
    public PaymentOffer getPaymentOfferById(@PathVariable Long id) {
        return paymentOfferService.getPaymentOfferById(id);
    }

    @PutMapping("/{id}")
    public PaymentOffer updatePaymentOffer(@PathVariable Long id, @RequestBody PaymentOfferDTO dto) {
        return paymentOfferService.updatePaymentOffer(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deletePaymentOffer(@PathVariable Long id) {
        paymentOfferService.deletePaymentOfferById(id);
    }


}
