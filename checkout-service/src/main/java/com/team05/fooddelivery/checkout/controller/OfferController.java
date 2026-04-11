
package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.model.Offer;
import com.team05.fooddelivery.checkout.service.OfferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    public ResponseEntity<Offer> createOffer(@RequestBody Offer offer){
        Offer newOffer = offerService.createOffer(offer);
        return ResponseEntity.status(HttpStatus.CREATED).body(newOffer);
    }

    @GetMapping
    public List<Offer> getAllOffers(){
        return offerService.getOffers();
    }

    @GetMapping("/{id}")
    public Offer getOfferById(@PathVariable Long id) {
        return offerService.getOfferById(id);
    }

    @PutMapping("/{id}")
    public Offer updateOffer(@PathVariable Long id, @RequestBody Offer offer) {
        return offerService.updateOffer(id, offer);
    }

    @DeleteMapping("/{id}")
    public void deleteOffer(@PathVariable Long id) {
        offerService.deleteOfferById(id);
    }

}
