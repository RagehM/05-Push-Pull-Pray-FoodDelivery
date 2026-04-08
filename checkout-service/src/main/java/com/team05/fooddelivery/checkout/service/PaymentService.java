package com.team05.fooddelivery.checkout.service;
import com.team05.fooddelivery.checkout.dto.RevenueReportDTO;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
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

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository =  paymentRepository;
    }

    // Payment CRUD
    public Payment createPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
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
        paymentRepository.deleteById(id);
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
        transactionDetails.put("refundAt", LocalDateTime.now().toString());
        payment.setStatus(PaymentStatus.REFUNDED);

        return paymentRepository.save(payment);
    }

    public RevenueReportDTO generateRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        if(endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is before end date");
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
