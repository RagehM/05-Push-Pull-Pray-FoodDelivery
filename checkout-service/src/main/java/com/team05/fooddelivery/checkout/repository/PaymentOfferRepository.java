package com.team05.fooddelivery.checkout.repository;

import com.team05.fooddelivery.checkout.model.PaymentOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOfferRepository extends JpaRepository<PaymentOffer, Long> {
}
