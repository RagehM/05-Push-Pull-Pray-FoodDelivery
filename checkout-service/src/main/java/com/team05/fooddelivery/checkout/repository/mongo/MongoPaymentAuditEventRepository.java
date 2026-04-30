package com.team05.fooddelivery.checkout.repository.mongo;

import com.team05.fooddelivery.checkout.model.mongo.PaymentAuditEvent;
import com.team05.shared.repository.mongo.MongoEventRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MongoPaymentAuditEventRepository extends MongoEventRepository<PaymentAuditEvent, String> {
    List<PaymentAuditEvent> findByAction(String action);
    List<PaymentAuditEvent> findByPaymentId(Long paymentId);
}