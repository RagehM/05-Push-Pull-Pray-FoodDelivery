package com.team05.fooddelivery.checkout.service;
import com.team05.fooddelivery.checkout.dto.ProcessPaymentRequestDTO;
import com.team05.fooddelivery.checkout.dto.AppliedOfferDTO;
import com.team05.fooddelivery.checkout.dto.PaymentDetailsDTO;
import com.team05.fooddelivery.checkout.dto.RevenueReportDTO;
import com.team05.fooddelivery.checkout.enums.OfferDiscountType;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Offer;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.model.PaymentOffer;
import com.team05.fooddelivery.checkout.repository.OfferRepository;
import com.team05.fooddelivery.checkout.repository.PaymentOfferRepository;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;
    private final PaymentOfferRepository paymentOfferRepository;

    public PaymentService(PaymentRepository paymentRepository,  OfferRepository offerRepository,  PaymentOfferRepository paymentOfferRepository) {
        this.paymentRepository =  paymentRepository;
        this.offerRepository = offerRepository;
        this.paymentOfferRepository = paymentOfferRepository;
    }

    // Payment CRUD
    public Payment createPayment(Payment payment) {
        if(paymentRepository.userExists(payment.getUserId()) == false) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if(paymentRepository.orderExists(payment.getOrderId()) == false) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return paymentRepository.save(payment);
    }

    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    public Payment updatePayment(Long id, Payment updatedPayment) {
        return paymentRepository.findById(id).map(payment -> {
            payment.setAmount(updatedPayment.getAmount());
            payment.setMethod(updatedPayment.getMethod());
            payment.setStatus(updatedPayment.getStatus());
            payment.setTransactionDetails(updatedPayment.getTransactionDetails());
            payment.setPaymentOffers(updatedPayment.getPaymentOffers());
            return paymentRepository.save(payment);
        }).orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public void deletePaymentById(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        paymentRepository.deleteById(id);
    }

    // S5-F3: User Payment Summary (DTO)
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

        return new UserPaymentSummaryDTO(userId, totalPayments, totalAmount, methodBreakdown);
    }

    // S5-F7: Retry Failed Payment (Transactional)
    @Transactional
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

        return paymentRepository.save(payment);
    }

    // S5-F1: Get Payments by Status and Date Range
    public List<Payment> getPaymentsByStatusAndDateRange(
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return paymentRepository.findByStatusAndDateRange(status == null ? null : status.name(), startDate, endDate);
    }

    @Transactional
    public Payment refundPayment(Long id, String reason) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        PaymentStatus status = payment.getStatus();

        if (!status.equals(PaymentStatus.COMPLETED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status not COMPLETED");
        }
        Map<String, Object> transactionDetails = payment.getTransactionDetails();
        transactionDetails.put("refundReason", reason);
        transactionDetails.put("refundedAt", LocalDateTime.now().toString());
        payment.setStatus(PaymentStatus.REFUNDED);

        return paymentRepository.save(payment);
    }

    @Transactional
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
        paymentRepository.save(payment);

        return payment;
    }

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

        RevenueReportDTO revenueReport = new RevenueReportDTO(
                totalRevenue,
                totalTransactions,
                averagePayment,
                refundedAmount,
                refundCount
        );

        return revenueReport;
    }

    // S5-F4: Process Payment for Order (Transactional)
    @Transactional
    public Payment processPaymentForOrder(Long orderId, ProcessPaymentRequestDTO dto) {

        // Guard 1: order must exist
        if (!paymentRepository.orderExists(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        // Guard 2: order must be DELIVERED
        String orderStatus = paymentRepository.findOrderStatusById(orderId);
        if (!"DELIVERED".equals(orderStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is not DELIVERED. Current status: " + orderStatus);
        }

        // Guard 3: no COMPLETED payment should exist for this order
        if (paymentRepository.completedPaymentExistsForOrder(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        // Guard 4: a PENDING payment must exist (created at delivery time)
        Payment payment = paymentRepository.findPendingPaymentByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No pending payment found for order: " + orderId));

        // Update payment method
        payment.setMethod(dto.method());

        // Mark as COMPLETED
        payment.setStatus(PaymentStatus.COMPLETED);

        // Populate JSONB transactionDetails
        Map<String, Object> details = payment.getTransactionDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        details.put("gatewayResponse", "approved");
        if (dto.cardLastFour() != null) {
            details.put("cardLastFour", dto.cardLastFour());
        }
        payment.setTransactionDetails(details);

        return paymentRepository.save(payment);
    }



    // S5-F8: Get Payment Details with Applied Offers
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

        return new PaymentDetailsDTO(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getTransactionDetails(),
                appliedOffers,
                totalDiscount,
                finalAmount
        );
    }
}
