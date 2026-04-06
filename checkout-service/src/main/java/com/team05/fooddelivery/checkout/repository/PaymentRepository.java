package com.team05.fooddelivery.checkout.repository;

import com.team05.fooddelivery.checkout.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
