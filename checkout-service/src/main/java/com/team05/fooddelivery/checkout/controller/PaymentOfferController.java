
package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.dto.OfferUsageDTO;
import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.dto.PaymentOfferDTO;
import com.team05.fooddelivery.checkout.service.PaymentOfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/payments/offers")
public class PaymentOfferController {

    private static final Logger log = LoggerFactory.getLogger(PaymentOfferController.class);

    private final PaymentOfferService  paymentOfferService;

    public PaymentOfferController(PaymentOfferService paymentOfferService) {
        this.paymentOfferService = paymentOfferService;
    }

    // S5-F9: GET /api/payments/offers/top-used?limit={n}
    @GetMapping("/top-used")
    public ResponseEntity<List<OfferUsageDTO>> getMostUsedOffers(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Received {} {}", "GET", "/api/payments/offers/top-used");
        List<OfferUsageDTO> result = paymentOfferService.getMostUsedOffers(limit);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/offers/top-used");
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<PaymentOffer> createPaymentOffer(@RequestBody PaymentOfferDTO dto) {
        log.info("Received {} {}", "POST", "/api/payments/offers");
        PaymentOffer paymentOffer = paymentOfferService.createPaymentOffer(dto);
        log.info("Returning {} for {} {}", HttpStatus.CREATED.value(), "POST", "/api/payments/offers");
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentOffer);
    }

    @GetMapping
    public List<PaymentOffer> getPaymentOffers() {
        log.info("Received {} {}", "GET", "/api/payments/offers");
        List<PaymentOffer> offers = paymentOfferService.getPaymentOffers();
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/offers");
        return offers;
    }

    @GetMapping("/{paymentOfferId}")
    public ResponseEntity<PaymentOffer> getPaymentOfferById(@PathVariable Long paymentOfferId) {
        log.info("Received {} {}", "GET", "/api/payments/offers/" + paymentOfferId);
        PaymentOffer offer = paymentOfferService.getPaymentOfferById(paymentOfferId);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/offers/" + paymentOfferId);
        return ResponseEntity.ok(offer);
    }

    @PutMapping("/{id}")
    public PaymentOffer updatePaymentOffer(@PathVariable Long id, @RequestBody PaymentOfferDTO dto) {
        log.info("Received {} {}", "PUT", "/api/payments/offers/" + id);
        PaymentOffer updated = paymentOfferService.updatePaymentOffer(id, dto);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "PUT", "/api/payments/offers/" + id);
        return updated;
    }

    @DeleteMapping("/{id}")
    public void deletePaymentOffer(@PathVariable Long id) {
        log.info("Received {} {}", "DELETE", "/api/payments/offers/" + id);
        paymentOfferService.deletePaymentOfferById(id);
        log.info("Returning {} for {} {}", HttpStatus.NO_CONTENT.value(), "DELETE", "/api/payments/offers/" + id);
    }

}

