package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // S5-F1: Get Payments by Status and Date Range
    public List<Payment> getPaymentsByStatusAndDateRange(
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return paymentRepository.findByStatusAndDateRange(status, startDate, endDate);
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
}
