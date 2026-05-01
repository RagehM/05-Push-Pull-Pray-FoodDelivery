package com.team05.fooddelivery.checkout.service;
import com.team05.fooddelivery.checkout.dto.ProcessPaymentRequestDTO;
import com.team05.fooddelivery.checkout.dto.AppliedOfferDTO;
import com.team05.fooddelivery.checkout.dto.PaymentDetailsDTO;
import com.team05.fooddelivery.checkout.dto.PaymentMethodDTO;
import com.team05.fooddelivery.checkout.dto.RevenueReportDTO;
import com.team05.fooddelivery.checkout.enums.OfferDiscountType;
import com.team05.fooddelivery.checkout.enums.PaymentMethod;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.factory.PaymentAuditEventFactory;
import com.team05.fooddelivery.checkout.model.Offer;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.model.mongo.PaymentAuditEvent;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import com.team05.fooddelivery.checkout.repository.PaymentOfferRepository;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import com.team05.fooddelivery.checkout.repository.mongo.MongoPaymentAuditEventRepository;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;
    private final PaymentOfferRepository paymentOfferRepository;
    private final MongoPaymentAuditEventRepository paymentAuditEventRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final PaymentAuditEventFactory paymentAuditEventFactory = new PaymentAuditEventFactory();

    public PaymentService(PaymentRepository paymentRepository,
                          OfferRepository offerRepository,
                          PaymentOfferRepository paymentOfferRepository,
                          MongoPaymentAuditEventRepository paymentAuditEventRepository) {
        this.paymentRepository = paymentRepository;
        this.offerRepository = offerRepository;
        this.paymentOfferRepository = paymentOfferRepository;
        this.paymentAuditEventRepository = paymentAuditEventRepository;
        this.observers.add(
                new MongoEventLogger<>(this.paymentAuditEventRepository, MongoEvent.EventType.PAYMENT_AUDIT, paymentAuditEventFactory)
        );
    }

    // Payment CRUD
    public Payment createPayment(Payment payment) {
        if(paymentRepository.userExists(payment.getUserId()) == false) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if(paymentRepository.orderExists(payment.getOrderId()) == false) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", savedPayment.getId());
        paymentAuditEvent.put("amount", savedPayment.getAmount());
        paymentAuditEvent.put("method", savedPayment.getMethod().name());
        paymentAuditEvent.put("action", "PAYMENT_CREATED");
        paymentAuditEvent.put("details", savedPayment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);

        return savedPayment;
    }

    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }

    @Cacheable(value = "checkout-service::payment", key = "#id")
    public Payment getPaymentById(Long id) {
        return paymentRepository.findByIdWithOffers(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Caching(
            put = @CachePut(value = "checkout-service::payment", key = "#id"),
            evict = {
                    @CacheEvict(value = "checkout-service::S5-F1", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F3", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F6", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F8", key = "#id"),
                    @CacheEvict(value = "checkout-service::S5-F9", key = "#id"),
                    @CacheEvict(value = "checkout-service::S5-F11", allEntries = true)
            }
    )
    public Payment updatePayment(Long id, Payment updatedPayment) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        payment.setAmount(updatedPayment.getAmount());
        payment.setMethod(updatedPayment.getMethod());
        payment.setStatus(updatedPayment.getStatus());
        payment.setTransactionDetails(updatedPayment.getTransactionDetails());
        payment.setPaymentOffers(updatedPayment.getPaymentOffers());

        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", savedPayment.getId());
        paymentAuditEvent.put("action", "PAYMENT_UPDATED");
        paymentAuditEvent.put("method", savedPayment.getMethod().name());
        paymentAuditEvent.put("amount", savedPayment.getAmount());
        paymentAuditEvent.put("details", savedPayment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);

        return savedPayment;
    }

    @Caching(evict = {
            @CacheEvict(value = "checkout-service::payment", key = "#id"),
            @CacheEvict(value = "checkout-service::S5-F1", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F3", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F6", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F8", key = "#id"),
            @CacheEvict(value = "checkout-service::S5-F9", key = "#id"),
            @CacheEvict(value = "checkout-service::S5-F11", allEntries = true)
    })
    public void deletePaymentById(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        paymentRepository.deleteById(id);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", payment.getId());
        paymentAuditEvent.put("amount", payment.getAmount());
        paymentAuditEvent.put("method", payment.getMethod().name());
        paymentAuditEvent.put("action", "PAYMENT_DELETED");
        paymentAuditEvent.put("details", payment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    // [S5-F1] Get Payments by Status and Date Range
    @Cacheable(
            value = "checkout-service::S5-F1",
            key = "T(String).valueOf(#status) + ':' + T(String).valueOf(#startDate) + ':' + T(String).valueOf(#endDate)"
    )
    public List<Payment> getPaymentsByStatusAndDateRange(
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return paymentRepository.findByStatusAndDateRange(status == null ? null : status.name(), startDate, endDate);
    }

    // [S5-F2] Process Refund (Transactional + JSONB Update)
    @Transactional
    @Caching(
            put = @CachePut(value = "checkout-service::payment", key = "#id"),
            evict = {
                    @CacheEvict(value = "checkout-service::S5-F1", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F3", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F6", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F8", key = "#id"),
                    @CacheEvict(value = "checkout-service::S5-F11", allEntries = true)
            }
    )
    public Payment refundPayment(Long id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only COMPLETED payments can be refunded");
        }

        Map<String, Object> transactionDetails = payment.getTransactionDetails();
        if (transactionDetails == null) {
            transactionDetails = new HashMap<>();
        }

        transactionDetails.put("refundReason", reason);
        transactionDetails.put("refundedAt", LocalDateTime.now().toString());

        payment.setTransactionDetails(transactionDetails);
        payment.setStatus(PaymentStatus.REFUNDED);

        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", savedPayment.getId());
        paymentAuditEvent.put("action", "REFUNDED");
        paymentAuditEvent.put("method", savedPayment.getMethod().name());
        paymentAuditEvent.put("amount", savedPayment.getAmount());
        paymentAuditEvent.put("details", savedPayment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);

        return savedPayment;
    }

    // [S5-F3] User Payment Summary (DTO)
    @Cacheable(value = "checkout-service::S5-F3", key = "#userId")
    public UserPaymentSummaryDTO getUserPaymentSummary(Long userId) {
        // Verify user exists via cross-service native SQL query
        long userCount = paymentRepository.countUsersById(userId);
        if (userCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        // Query COMPLETED payments grouped by method
        List<Object[]> rows = paymentRepository.findCompletedPaymentSummaryByUserId(userId);

        Map<String, Double> methodBreakdown = new HashMap<>();
        long totalPayments = 0L;
        double totalAmount = 0.0;

        for (Object[] row : rows) {
            String method = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double sum = ((Number) row[2]).doubleValue();

            methodBreakdown.put(method, sum);
            totalPayments += count;
            totalAmount += sum;
        }

        return UserPaymentSummaryDTO.builder()
                .userId(userId)
                .totalPayments(totalPayments)
                .totalAmount(totalAmount)
                .methodBreakdown(methodBreakdown)
                .build();
    }

    // [S5-F4] Process Payment for Order (Transactional)
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "checkout-service::S5-F1", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F3", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F6", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F8", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F11", allEntries = true)
    })
    public Payment processPaymentForOrder(Long orderId, ProcessPaymentRequestDTO dto, boolean simulateFailure) {

        // Guard 1: order must exist
        if (!paymentRepository.orderExists(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        // Guard 2: order must be DELIVERED
        String orderStatus = paymentRepository.findOrderStatusById(orderId);
        if (!"DELIVERED".equals(orderStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order is not DELIVERED. Current status: " + orderStatus
            );
        }

        // Guard 3: no COMPLETED payment should exist
        if (paymentRepository.completedPaymentExistsForOrder(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order already paid");
        }

        // Guard 4: must have a PENDING payment
        Payment payment = paymentRepository.findPendingPaymentByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No pending payment found for order: " + orderId
                ));

        Map<String, Object> createdPaymentAuditEvent = new HashMap<>();
        createdPaymentAuditEvent.put("paymentId", payment.getId());
        createdPaymentAuditEvent.put("amount", payment.getAmount());
        createdPaymentAuditEvent.put("method", payment.getMethod().name());
        createdPaymentAuditEvent.put("action", "CREATED");
        createdPaymentAuditEvent.put("details", payment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", createdPaymentAuditEvent);

        Map<String, Object> transactionDetails = payment.getTransactionDetails();
        if (transactionDetails == null) {
            transactionDetails = new HashMap<>();
        }

        if (dto != null) {
            if (dto.cardLastFour() != null) {
                transactionDetails.put("cardLastFour", dto.cardLastFour());
            }
            if (dto.method() != null) {
                payment.setMethod(dto.method());
            }
        }

        String action;
        if (simulateFailure) {
            payment.setStatus(PaymentStatus.FAILED);
            transactionDetails.put("gatewayResponse", "declined");
            action = "FAILED";
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
            transactionDetails.put("gatewayResponse", "approved");
            action = "COMPLETED";
        }

        payment.setTransactionDetails(transactionDetails);

        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", savedPayment.getId());
        paymentAuditEvent.put("amount", savedPayment.getAmount());
        paymentAuditEvent.put("method", savedPayment.getMethod().name());
        paymentAuditEvent.put("action", action);
        paymentAuditEvent.put("details", savedPayment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);

        return savedPayment;
    }

    // [S5-F5] Apply Offer to Payment (Transactional + Join Entity)
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "checkout-service::payment", key = "#paymentId"),
            @CacheEvict(value = "checkout-service::offer", key = "#offerId"),
            @CacheEvict(value = "checkout-service::S5-F1", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F3", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F6", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F8", key = "#paymentId"),
            @CacheEvict(value = "checkout-service::S5-F9", allEntries = true),
            @CacheEvict(value = "checkout-service::S5-F11", allEntries = true)
    })
    public Payment applyOfferToPayment(Long paymentId, Long offerId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cannot apply offer to a completed/cancelled payment"));

        if(payment.getStatus().equals(PaymentStatus.COMPLETED) ||  payment.getStatus().equals(PaymentStatus.REFUNDED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status cant be COMPLETED or REFUNDED");
        }

        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "offer not found"));

        if(offer.getActive() == false) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offer not active");
        }
        if(offer.getExpiryDate().isBefore(LocalDateTime.now()) ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offer expired");
        }
        if(offer.getCurrentUses() >= offer.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offer usage limit reached");
        }

        List<PaymentOffer> paymentOffers = offer.getPaymentOffers();
        boolean alreadyApplied = paymentOffers.stream().anyMatch(paymentOffer -> paymentOffer.getPayment().getId().equals(paymentId));
        if (alreadyApplied) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offer already applied");
        }

        OfferDiscountType discountType = offer.getDiscountType();
        Double discount = 0.0;
        if(discountType == OfferDiscountType.PERCENTAGE) {
            discount = payment.getAmount() * (offer.getDiscountValue() / 100);
        }
        if(discountType == OfferDiscountType.FIXED) {
            discount = offer.getDiscountValue();
        }
        if(discount > payment.getAmount()) {
            discount = payment.getAmount();
        }

        PaymentOffer newPaymentOffer = new PaymentOffer();
        newPaymentOffer.setDiscountApplied(discount);
        newPaymentOffer.setAppliedAt(LocalDateTime.now());
        newPaymentOffer.setPayment(payment);
        newPaymentOffer.setOffer(offer);


        offer.setCurrentUses(offer.getCurrentUses() + 1);

        paymentOfferRepository.save(newPaymentOffer);
        offerRepository.save(offer);
        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", savedPayment.getId());
        paymentAuditEvent.put("amount", savedPayment.getAmount());
        paymentAuditEvent.put("method", savedPayment.getMethod().name());
        paymentAuditEvent.put("action", "OFFER_APPLIED");
        paymentAuditEvent.put("details", savedPayment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);

        return savedPayment;
    }

    // [S5-F6] Revenue Report by Date Range (Report DTO)
    @Cacheable(value = "checkout-service::S5-F6", key = "#startDate.toString() + ':' + #endDate.toString()")
    public RevenueReportDTO generateRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        if(endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date is before Start date");
        }
        List<Payment> paymentsList = paymentRepository.findByDateRange(startDate, endDate);

        List<Payment> completedPayments = paymentsList.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).toList();

        Double totalRevenue = completedPayments.stream().mapToDouble(Payment::getAmount).sum();

        Integer totalTransactions = completedPayments.size();

        Double averagePayment = totalTransactions > 0 ? totalRevenue / totalTransactions : 0.0;

        List<Payment> refundedPayments = paymentsList.stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED).toList();

        Double refundedAmount = refundedPayments.stream().mapToDouble(Payment::getAmount).sum();

        Integer refundCount = refundedPayments.size();

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .averagePayment(averagePayment)
                .refundedAmount(refundedAmount)
                .refundCount(refundCount)
                .build();
    }

    // [S5-F7] Retry Failed Payment (Transactional)
    @Transactional
    @Caching(
            put = @CachePut(value = "checkout-service::payment", key = "#id"),
            evict = {
                    @CacheEvict(value = "checkout-service::S5-F1", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F3", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F6", allEntries = true),
                    @CacheEvict(value = "checkout-service::S5-F8", key = "#id"),
                    @CacheEvict(value = "checkout-service::S5-F11", allEntries = true)
            }
    )
    public Payment retryFailedPayment(Long id) {
        // Find payment – 404 if not found
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found with id: " + id));

        // Validate status must be FAILED – 400 otherwise
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment cannot be retried. Current status: " + payment.getStatus());
        }

        // Update status to COMPLETED
        payment.setStatus(PaymentStatus.COMPLETED);

        // Update JSONB transactionDetails
        Map<String, Object> details = payment.getTransactionDetails();
        if (details == null) {
            details = new HashMap<>();
        }

        // Increment retryAttempt (default 0 if missing)
        int retryAttempt = 0;
        if (details.containsKey("retryAttempt")) {
            retryAttempt = ((Number) details.get("retryAttempt")).intValue();
        }
        details.put("retryAttempt", retryAttempt + 1);

        // Overwrite gatewayResponse with "approved"
        details.put("gatewayResponse", "approved");

        payment.setTransactionDetails(details);

        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> paymentAuditEvent = new HashMap<>();
        paymentAuditEvent.put("paymentId", savedPayment.getId());
        paymentAuditEvent.put("amount", savedPayment.getAmount());
        paymentAuditEvent.put("method", savedPayment.getMethod().name());
        paymentAuditEvent.put("action", "RETRY_ATTEMPTED");
        paymentAuditEvent.put("details", savedPayment.getTransactionDetails());

        notifyObservers("PAYMENT_AUDIT", paymentAuditEvent);

        return savedPayment;
    }

    // [S5-F8] Get Payment Details with Applied Offers (Join Entity DTO)
    @Cacheable(value = "checkout-service::S5-F8",key = "#paymentId")
    public PaymentDetailsDTO getPaymentDetails(Long paymentId) {
        // Fetch payment with offers eagerly loaded via JOIN FETCH
        Payment payment = paymentRepository.findByIdWithOffers(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        // Map each PaymentOffer join entity → AppliedOfferDTO
        List<AppliedOfferDTO> appliedOffers = payment.getPaymentOffers().stream()
                .map(po -> new AppliedOfferDTO(
                        po.getOffer().getCode(),
                        po.getOffer().getDiscountType(),
                        po.getDiscountApplied(),
                        po.getAppliedAt()
                ))
                .toList();

        // Aggregate discount
        Double totalDiscount = appliedOffers.stream()
                .mapToDouble(AppliedOfferDTO::discountApplied)
                .sum();

        Double finalAmount = Math.max(0.0, payment.getAmount() - totalDiscount);

        return PaymentDetailsDTO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .originalAmount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionDetails(payment.getTransactionDetails())
                .appliedOffers(appliedOffers)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
    }

    // [S5-F11] Get Payment Method Breakdown
    //
    // Reads the payment_audit_trail MongoDB collection, filtered by timestamp in
    // the inclusive range [startDate T00:00:00, endDate T23:59:59.999] and by
    // action ∈ {COMPLETED, FAILED}. Groups by `method`, computing per-method
    // successCount / failureCount / successRate / totalAmount. Auth (USER role)
    // is enforced at the controller / SecurityConfig level. Result is cached
    // for 10 minutes under checkout-service::S5-F11::<startDate>:<endDate>.
    @Cacheable(
            value = "checkout-service::S5-F11",
            key = "T(String).valueOf(#startDate) + ':' + T(String).valueOf(#endDate)"
    )
    public List<PaymentMethodDTO> getPaymentMethodBreakdown(LocalDate startDate, LocalDate endDate) {
        // (b) Validate the date range — 400 if startDate is after endDate.
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must be on or before endDate");
        }

        // Expand to a fully-closed [00:00:00, 23:59:59.999] window matching
        // server-time-zone PG TIMESTAMPS / Mongo timestamps.
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.atTime(23, 59, 59, 999_000_000);

        // (c) Pull only COMPLETED + FAILED audit events in the window.
        Set<String> actions = Set.of(
                PaymentAuditEvent.Actions.COMPLETED,
                PaymentAuditEvent.Actions.FAILED
        );
        List<PaymentAuditEvent> events =
                paymentAuditEventRepository.findByActionInAndTimestampBetween(actions, start, end);

        if (events == null || events.isEmpty()) {
            // (f) Empty list when no data — do NOT 404.
            return List.of();
        }

        // (d) Aggregate per method. EnumMap keeps the result deterministic and
        // skips events whose `method` is missing or unrecognised — per the
        // M2 spec, such events "silently vanish from the breakdown".
        Map<PaymentMethod, long[]>   counts = new EnumMap<>(PaymentMethod.class); // [success, failure]
        Map<PaymentMethod, Double>   totals = new EnumMap<>(PaymentMethod.class); // sum of COMPLETED amounts

        for (PaymentAuditEvent ev : events) {
            String rawMethod = ev.getMethod();
            if (rawMethod == null || ev.getAmount() == null) {
                continue;
            }
            PaymentMethod method;
            try {
                method = PaymentMethod.valueOf(rawMethod);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            long[] c = counts.computeIfAbsent(method, k -> new long[2]);
            if (PaymentAuditEvent.Actions.COMPLETED.equals(ev.getAction())) {
                c[0] += 1; // successCount
                totals.merge(method, ev.getAmount(), Double::sum);
            } else if (PaymentAuditEvent.Actions.FAILED.equals(ev.getAction())) {
                c[1] += 1; // failureCount
            }
        }

        List<PaymentMethodDTO> result = new ArrayList<>(counts.size());
        for (Map.Entry<PaymentMethod, long[]> entry : counts.entrySet()) {
            long success = entry.getValue()[0];
            long failure = entry.getValue()[1];
            long denom   = success + failure;
            double rate  = denom == 0 ? 0.0 : (double) success / denom;
            double total = totals.getOrDefault(entry.getKey(), 0.0);

            result.add(new PaymentMethodDTO(entry.getKey(), success, failure, rate, total));
        }
        return result;
    }
}