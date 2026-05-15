package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.dto.PaymentDetailsDTO;
import com.team05.fooddelivery.checkout.dto.PaymentMethodDTO;
import com.team05.fooddelivery.checkout.dto.ProcessPaymentRequestDTO;
import com.team05.fooddelivery.checkout.dto.RevenueReportDTO;
import com.team05.fooddelivery.checkout.dto.*;
import com.team05.fooddelivery.checkout.dto.CuisineRevenueDTO;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;


    public PaymentController(PaymentService paymentService) {

        this.paymentService = paymentService;
    }
    // Payment CRUD
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        Payment newPayment = paymentService.createPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(newPayment);
    }

    @GetMapping
    public List<Payment> getPayments() {
        return paymentService.getPayments();
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @PutMapping("/{id}")
    public Payment updatePayment(@PathVariable Long id, @RequestBody Payment payment) {
        return paymentService.updatePayment(id, payment);
    }

    @DeleteMapping("/{id}")
    public void deletePayment(@PathVariable Long id) {
        paymentService.deletePaymentById(id);
    }

    // S5-F1: GET /api/payments/search?status={s}&startDate={d}&endDate={d}
    @GetMapping("/search")
    public ResponseEntity<List<Payment>> searchPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end   = (endDate   != null) ? endDate.atTime(23, 59, 59) : null;

        List<Payment> results = paymentService.getPaymentsByStatusAndDateRange(status, start, end);
        return ResponseEntity.ok(results);
    }

    // S5-F2: PUT /api/payments/{id}/refund
    @PutMapping("/{id}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long id, @RequestBody String reason) {
        Payment refundedPayment = paymentService.refundPayment(id, reason);
        return ResponseEntity.ok(refundedPayment);
    }

    // S5-F3: GET /api/payments/user/{userId}/summary
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserPaymentSummaryDTO> getUserPaymentSummary(@PathVariable Long userId) {
        UserPaymentSummaryDTO summary = paymentService.getUserPaymentSummary(userId);
        return ResponseEntity.ok(summary);
    }

    // S5-F4: POST /api/payments/order/{orderId}
    @PostMapping("/order/{orderId}")
    public ResponseEntity<Payment> processPaymentForOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) ProcessPaymentRequestDTO dto,
            @RequestParam(required = false) boolean simulateFailure) {
        Payment payment = paymentService.processPaymentForOrder(orderId, dto, simulateFailure);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    // S5-F5: POST /api/payments/{paymentId}/offers/{offerId}
    @PostMapping("/{paymentId}/offers/{offerId}")
    public ResponseEntity<Payment> applyOfferToPayment(@PathVariable Long paymentId, @PathVariable Long offerId) {
        Payment updatedPayment = paymentService.applyOfferToPayment(paymentId, offerId);
        return ResponseEntity.ok(updatedPayment);
    }

    // S5-F6: GET /api/payments/reports/revenue?startDate={d}&endDate={d}
    @GetMapping("/reports/revenue")
    public ResponseEntity<RevenueReportDTO> generateRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end   = (endDate   != null) ? endDate.atTime(23, 59, 59) : null;

        RevenueReportDTO report = paymentService.generateRevenueReport(start, end);
        return ResponseEntity.ok(report);
    }

    // S5-F7: PUT /api/payments/{id}/retry
    @PutMapping("/{id}/retry")
    public ResponseEntity<Payment> retryFailedPayment(@PathVariable Long id) {
        Payment updated = paymentService.retryFailedPayment(id);
        return ResponseEntity.ok(updated);
    }

    // S5-F8: GET /api/payments/{paymentId}/details
    @GetMapping("/{paymentId}/details")
    public ResponseEntity<PaymentDetailsDTO> getPaymentDetails(@PathVariable Long paymentId) {
        PaymentDetailsDTO details = paymentService.getPaymentDetails(paymentId);
        return ResponseEntity.ok(details);
    }

    // S5-F10: GET /api/payments/analytics/cuisine?startDate={d}&endDate={d}
    @GetMapping("/analytics/cuisine")
    public ResponseEntity<List<CuisineRevenueDTO>> getRevenueByCuisine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<CuisineRevenueDTO> result = paymentService.getRevenueByCuisine(start, end);
        return ResponseEntity.ok(result);
    }

    // S5-F11: GET /api/payments/analytics/methods?startDate={d}&endDate={d}
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/analytics/methods")
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethodBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<PaymentMethodDTO> breakdown =
                paymentService.getPaymentMethodBreakdown(startDate, endDate);
        return ResponseEntity.ok(breakdown);
    }

    // [S5-F12] Process Order Refund with Delivery Fee Handling
    @PostMapping("/{id}/refund-with-fee-handling")
    public ResponseEntity<Payment> processOrderRefundWithDeliveryFeeHandling(
            @PathVariable Long id,
            @RequestBody RefundRequest refundRequest) {
        return paymentService.processOrderRefundWithDeliveryFeeHandling(id, refundRequest);
    }

    // [S5-READ-DB / S1-F6] GET /api/payments/user/{userId}/total?startDate={d}&endDate={d}
    // Returns total COMPLETED payment amount for this user in the date range.
    // 0.0 if no payments.
    @GetMapping("/user/{userId}/total")
    public ResponseEntity<BigDecimal> getUserPaymentTotal(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("HTTP GET /api/payments/user/{}/total startDate={} endDate={}", userId, startDate, endDate);
        BigDecimal total = paymentService.getUserPaymentTotal(userId, startDate, endDate);
        return ResponseEntity.ok(total);
    }

}
