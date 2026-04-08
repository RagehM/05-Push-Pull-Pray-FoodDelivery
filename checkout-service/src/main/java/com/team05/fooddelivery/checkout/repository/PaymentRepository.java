package com.team05.fooddelivery.checkout.repository;

import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // S5-F3: Check if user exists (cross-service via native SQL on shared DB)
    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    long countUsersById(@Param("userId") Long userId);

    // S5-F3: Fetch COMPLETED payments for a user grouped by method
    // Returns Object[] rows: [method (String), count (Long), sum (Double)]
    @Query(value = "SELECT p.method, COUNT(p.id), SUM(p.amount) " +
                   "FROM payments p " +
                   "WHERE p.user_id = :userId AND p.status = 'COMPLETED' " +
                   "GROUP BY p.method",
           nativeQuery = true)
    List<Object[]> findCompletedPaymentSummaryByUserId(@Param("userId") Long userId);
}
