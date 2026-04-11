package com.team05.fooddelivery.checkout.repository;

import com.team05.fooddelivery.checkout.model.Offer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    // S5-F9: Offers that appear in payment_offers, aggregated
    // Uses Pageable for the LIMIT clause (safer than named param in LIMIT)
    @Query(value = "SELECT o.id, o.code, o.discount_type, o.discount_value, o.current_uses, " +
            "COALESCE(SUM(po.discount_applied), 0) AS total_discount, " +
            "o.active, o.expiry_date " +
            "FROM offers o " +
            "JOIN payment_offers po ON o.id = po.offer_id " +
            "GROUP BY o.id, o.code, o.discount_type, o.discount_value, " +
            "         o.current_uses, o.active, o.expiry_date " +
            "ORDER BY o.current_uses DESC",
            nativeQuery = true)
    List<Object[]> findMostUsedOffers(Pageable pageable);
}