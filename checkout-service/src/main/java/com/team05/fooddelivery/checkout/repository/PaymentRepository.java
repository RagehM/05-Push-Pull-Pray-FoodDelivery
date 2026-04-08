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
}
