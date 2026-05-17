
package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.model.Offer;
import com.team05.fooddelivery.checkout.service.OfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private static final Logger log = LoggerFactory.getLogger(OfferController.class);

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    public ResponseEntity<Offer> createOffer(@RequestBody Offer offer){
        log.info("Received {} {}", "POST", "/api/offers");
        Offer newOffer = offerService.createOffer(offer);
        log.info("Returning {} for {} {}", HttpStatus.CREATED.value(), "POST", "/api/offers");
        return ResponseEntity.status(HttpStatus.CREATED).body(newOffer);
    }

    @GetMapping
    public List<Offer> getAllOffers(){
        log.info("Received {} {}", "GET", "/api/offers");
        List<Offer> offers = offerService.getOffers();
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/offers");
        return offers;
    }

    @GetMapping("/{id}")
    public Offer getOfferById(@PathVariable Long id) {
        log.info("Received {} {}", "GET", "/api/offers/" + id);
        Offer offer = offerService.getOfferById(id);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/offers/" + id);
        return offer;
    }

    @PutMapping("/{id}")
    public Offer updateOffer(@PathVariable Long id, @RequestBody Offer offer) {
        log.info("Received {} {}", "PUT", "/api/offers/" + id);
        Offer updated = offerService.updateOffer(id, offer);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "PUT", "/api/offers/" + id);
        return updated;
    }

    @DeleteMapping("/{id}")
    public void deleteOffer(@PathVariable Long id) {
        log.info("Received {} {}", "DELETE", "/api/offers/" + id);
        offerService.deleteOfferById(id);
        log.info("Returning {} for {} {}", HttpStatus.NO_CONTENT.value(), "DELETE", "/api/offers/" + id);
    }

}
