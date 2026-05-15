package com.team05.fooddelivery.checkout.repository;

import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // S5-F3: Fetch COMPLETED payments for a user grouped by method
    // Returns Object[] rows: [method (String), count (Long), sum (Double)]
    @Query(value = "SELECT p.method, COUNT(p.id), SUM(p.amount) " +
                   "FROM payments p " +
                   "WHERE p.user_id = :userId AND p.status = 'COMPLETED' " +
                   "GROUP BY p.method",
           nativeQuery = true)
    List<Object[]> findCompletedPaymentSummaryByUserId(@Param("userId") Long userId);

    boolean existsByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findLockedFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findAllByUserIdAndStatus(Long userId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status AND p.createdAt >= :startDate AND p.createdAt <= :endDate ORDER BY p.createdAt DESC")
    List<Payment> findAllByUserIdAndStatusAndCreatedAtBetween(@Param("userId") Long userId,
                                                              @Param("status") PaymentStatus status,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt >= :startDate AND p.createdAt <= :endDate ORDER BY p.createdAt DESC")
    List<Payment> findAllByStatusAndCreatedAtBetween(@Param("status") PaymentStatus status,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    // S5-F8:The findByIdWithOffers JPQL query (added in S5-F4 section) uses LEFT JOIN FETCH to eagerly load the paymentOffers collection
    // and the nested offer in a single round-trip, avoiding LazyInitializationException:
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.paymentOffers po LEFT JOIN FETCH po.offer WHERE p.id = :id")
    Optional<Payment> findByIdWithOffers(@Param("id") Long id);
}
