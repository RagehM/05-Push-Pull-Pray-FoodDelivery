package com.team05.fooddelivery.checkout.repository;

import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // S5-F1: Get Payments by Status and Date Range (all params optional)
    @Query(value = "SELECT * FROM payments p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(CAST(:startDate AS timestamp) IS NULL OR p.created_at >= CAST(:startDate AS timestamp)) AND " +
            "(CAST(:endDate AS timestamp) IS NULL OR p.created_at <= CAST(:endDate AS timestamp)) " +
            "ORDER BY p.created_at DESC",
            nativeQuery = true)
    List<Payment> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT * FROM payments p WHERE " +
            "(CAST(:startDate AS timestamp) IS NULL OR p.created_at >= CAST(:startDate AS timestamp)) AND " +
            "(CAST(:endDate AS timestamp) IS NULL OR p.created_at <= CAST(:endDate AS timestamp)) " +
            "ORDER BY p.created_at DESC",
            nativeQuery = true)
    List<Payment> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

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


    @Query(value = "SELECT COUNT(*) > 0 FROM orders WHERE id = :orderId", nativeQuery = true)
    boolean orderExists(@Param("orderId") Long orderId);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean userExists(@Param("userId") Long userId);

    // S5-F4: Process Payment for Order (Transactional)
    @Query(value = "SELECT * FROM payments WHERE order_id = :orderId AND status = 'PENDING' LIMIT 1",
            nativeQuery = true)
    Optional<Payment> findPendingPaymentByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT COUNT(*) > 0 FROM payments WHERE order_id = :orderId AND status = 'COMPLETED'",
            nativeQuery = true)
    boolean completedPaymentExistsForOrder(@Param("orderId") Long orderId);

    @Query(value = "SELECT status FROM orders WHERE id = :orderId",
            nativeQuery = true)
    String findOrderStatusById(@Param("orderId") Long orderId);

    // S5-F4: Fetch delivery fee inputs: order metadata.deliveryType, total_amount, and restaurant details.deliveryFee
    @Query(value = "SELECT o.metadata->>'deliveryType', o.total_amount, r.details->>'deliveryFee' " +
                   "FROM orders o " +
                   "LEFT JOIN restaurants r ON r.id = o.restaurant_id " +
                   "WHERE o.id = :orderId",
           nativeQuery = true)
    List<Object[]> findOrderDeliveryData(@Param("orderId") Long orderId);

    // S5-F8:The findByIdWithOffers JPQL query (added in S5-F4 section) uses LEFT JOIN FETCH to eagerly load the paymentOffers collection
    // and the nested offer in a single round-trip, avoiding LazyInitializationException:
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.paymentOffers po LEFT JOIN FETCH po.offer WHERE p.id = :id")
    Optional<Payment> findByIdWithOffers(@Param("id") Long id);

    // [S5-READ-DB] Aggregate COMPLETED payments for a user in an optional date range.
    // Pure checkout-postgres query — no cross-DB JOINs. User existence is verified via
    // Feign -> user-service in the service layer.
    // Returns Object[] rows: [method (String), count (Long), sum (Numeric)]
    @Query(value = "SELECT p.method, COUNT(p.id), COALESCE(SUM(p.amount), 0) " +
                   "FROM payments p " +
                   "WHERE p.user_id = :userId AND p.status = 'COMPLETED' " +
                   "  AND (CAST(:startDate AS timestamp) IS NULL OR p.created_at >= CAST(:startDate AS timestamp)) " +
                   "  AND (CAST(:endDate   AS timestamp) IS NULL OR p.created_at <= CAST(:endDate   AS timestamp)) " +
                   "GROUP BY p.method",
           nativeQuery = true)
    List<Object[]> findCompletedPaymentTotalsByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // S5-F10: Revenue by cuisine type with delivery fee breakdown (cross-service native SQL JOIN)
    @Query(value = "SELECT r.cuisine_type, COUNT(DISTINCT p.order_id), SUM(p.amount), " +
            "SUM(COALESCE(CAST(p.transaction_details->>'deliveryFee' AS NUMERIC), p.amount * 0.10)) " + //is a JSONB column and deliveryFee may not be present for all payments. When absent, 10% of the payment amount is used as a fallback estimate.
            "FROM payments p " +
            "JOIN orders o ON o.id = p.order_id " +
            "JOIN restaurants r ON r.id = o.restaurant_id " +
            "WHERE o.order_date >= :startDate AND o.order_date <= :endDate AND p.status = 'COMPLETED' " +
            "GROUP BY r.cuisine_type",
            nativeQuery = true)
    List<Object[]> findRevenueByCuisineAndDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
