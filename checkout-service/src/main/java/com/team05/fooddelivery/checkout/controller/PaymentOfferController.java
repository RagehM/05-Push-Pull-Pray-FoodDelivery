
package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.dto.PaymentOfferDTO;
import com.team05.fooddelivery.checkout.service.PaymentOfferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/payments/offers")
public class PaymentOfferController {
    private final PaymentOfferService  paymentOfferService;

    public PaymentOfferController(PaymentOfferService paymentOfferService) {
        this.paymentOfferService = paymentOfferService;
    }

    @PostMapping
    public ResponseEntity<PaymentOffer> createPaymentOffer(@RequestBody PaymentOfferDTO dto) {
        PaymentOffer paymentOffer = paymentOfferService.createPaymentOffer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentOffer);
    }

    @GetMapping
    public List<PaymentOffer> getPaymentOffers() {
        return paymentOfferService.getPaymentOffers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentOffer> getPaymentOfferById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentOfferService.getPaymentOfferById(id));
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

