package com.team05.fooddelivery.checkout.repository.mongo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.checkout.model.mongo.PaymentAuditEvent;

/**
 * MongoDB repository for the payment_audit_trail collection.
 *
 * Used by S5-F11 (Get Payment Method Breakdown) to fetch the COMPLETED
 * and FAILED audit events whose {@code timestamp} falls in the requested
 * fully-closed range.
 */
@Repository
public interface MongoPaymentAuditEventRepository
        extends MongoRepository<PaymentAuditEvent, String> {

    /**
     * Returns every event whose action is in {@code actions} and whose
     * timestamp lies in the inclusive range [start, end]. Documents whose
     * {@code method} or {@code amount} is null are still returned: S5-F11
     * filters them out application-side so the repository remains generic.
     */
    List<PaymentAuditEvent> findByActionInAndTimestampBetween(
            Collection<String> actions,
            LocalDateTime start,
            LocalDateTime end);
}
