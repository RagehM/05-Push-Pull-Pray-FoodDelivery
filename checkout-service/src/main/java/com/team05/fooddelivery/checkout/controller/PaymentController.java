package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.dto.*;
import com.team05.fooddelivery.checkout.dto.PaymentDetailsDTO;
import com.team05.fooddelivery.checkout.dto.PaymentMethodDTO;
import com.team05.fooddelivery.checkout.dto.ProcessPaymentRequestDTO;
import com.team05.fooddelivery.checkout.dto.RevenueReportDTO;
import com.team05.fooddelivery.checkout.dto.CuisineRevenueDTO;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        log.info("Received {} {}", "POST", "/api/payments");
        Payment newPayment = paymentService.createPayment(payment);
        log.info("Returning {} for {} {}", HttpStatus.CREATED.value(), "POST", "/api/payments");
        return ResponseEntity.status(HttpStatus.CREATED).body(newPayment);
    }

    @GetMapping
    public List<Payment> getPayments() {
        log.info("Received {} {}", "GET", "/api/payments");
        List<Payment> payments = paymentService.getPayments();
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments");
        return payments;
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Long id) {
        log.info("Received {} {}", "GET", "/api/payments/" + id);
        Payment payment = paymentService.getPaymentById(id);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/" + id);
        return payment;
    }

    @PutMapping("/{id}")
    public Payment updatePayment(@PathVariable Long id, @RequestBody Payment payment) {
        log.info("Received {} {}", "PUT", "/api/payments/" + id);
        Payment updated = paymentService.updatePayment(id, payment);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "PUT", "/api/payments/" + id);
        return updated;
    }

    @DeleteMapping("/{id}")
    public void deletePayment(@PathVariable Long id) {
        log.info("Received {} {}", "DELETE", "/api/payments/" + id);
        paymentService.deletePaymentById(id);
        log.info("Returning {} for {} {}", HttpStatus.NO_CONTENT.value(), "DELETE", "/api/payments/" + id);
    }

    // S5-F1: GET /api/payments/search?status={s}&startDate={d}&endDate={d}
    @GetMapping("/search")
    public ResponseEntity<List<Payment>> searchPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Received {} {}", "GET", "/api/payments/search");
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end   = (endDate   != null) ? endDate.atTime(23, 59, 59) : null;

        List<Payment> results = paymentService.getPaymentsByStatusAndDateRange(status, start, end);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/search");
        return ResponseEntity.ok(results);
    }

    // S5-F2: PUT /api/payments/{id}/refund
    @PutMapping("/{id}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long id, @RequestBody String reason) {
        log.info("Received {} {}", "PUT", "/api/payments/" + id + "/refund");
        Payment refundedPayment = paymentService.refundPayment(id, reason);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "PUT", "/api/payments/" + id + "/refund");
        return ResponseEntity.ok(refundedPayment);
    }

    // S5-F3: GET /api/payments/user/{userId}/summary
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<UserPaymentSummaryDTO> getUserPaymentSummary(@PathVariable Long userId) {
        log.info("Received {} {}", "GET", "/api/payments/user/" + userId + "/summary");
        UserPaymentSummaryDTO summary = paymentService.getUserPaymentSummary(userId);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/user/" + userId + "/summary");
        return ResponseEntity.ok(summary);
    }

    // M3 S5-F4: POST /api/payments/process
    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(
            @RequestBody ProcessPaymentRequestDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterUserId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole,
            @RequestParam(required = false) boolean simulateFailure) {
        log.info("Received {} {}", "POST", "/api/payments/process");
        authorizePaymentRequest(dto, requesterUserId, requesterRole);
        Payment payment = paymentService.processPaymentForOrder(dto.orderId(), dto, simulateFailure);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "POST", "/api/payments/process");
        return ResponseEntity.ok(payment);
    }

    // S5-F5: POST /api/payments/{paymentId}/offers/{offerId}
    @PostMapping("/{paymentId}/offers/{offerId}")
    public ResponseEntity<Payment> applyOfferToPayment(@PathVariable Long paymentId, @PathVariable Long offerId) {
        log.info("Received {} {}", "POST", "/api/payments/" + paymentId + "/offers/" + offerId);
        Payment updatedPayment = paymentService.applyOfferToPayment(paymentId, offerId);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "POST", "/api/payments/" + paymentId + "/offers/" + offerId);
        return ResponseEntity.ok(updatedPayment);
    }

    // S5-F6: GET /api/payments/reports/revenue?startDate={d}&endDate={d}
    @GetMapping("/reports/revenue")
    public ResponseEntity<RevenueReportDTO> generateRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Received {} {}", "GET", "/api/payments/reports/revenue");
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end   = endDate.atTime(23, 59, 59);

        RevenueReportDTO report = paymentService.generateRevenueReport(start, end);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/reports/revenue");
        return ResponseEntity.ok(report);
    }

    // S5-F7: PUT /api/payments/{id}/retry
    @PutMapping("/{id}/retry")
    public ResponseEntity<Payment> retryFailedPayment(@PathVariable Long id) {
        log.info("Received {} {}", "PUT", "/api/payments/" + id + "/retry");
        Payment updated = paymentService.retryFailedPayment(id);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "PUT", "/api/payments/" + id + "/retry");
        return ResponseEntity.ok(updated);
    }

    // S5-F8: GET /api/payments/{paymentId}/details
    @GetMapping("/{paymentId}/details")
    public ResponseEntity<PaymentDetailsDTO> getPaymentDetails(@PathVariable Long paymentId) {
        log.info("Received {} {}", "GET", "/api/payments/" + paymentId + "/details");
        PaymentDetailsDTO details = paymentService.getPaymentDetails(paymentId);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/" + paymentId + "/details");
        return ResponseEntity.ok(details);
    }

    // S5-F10: GET /api/payments/analytics/cuisine?startDate={d}&endDate={d}
    @GetMapping("/analytics/cuisine")
    public ResponseEntity<List<CuisineRevenueDTO>> getRevenueByCuisine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Received {} {}", "GET", "/api/payments/analytics/cuisine");
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<CuisineRevenueDTO> result = paymentService.getRevenueByCuisine(start, end);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/analytics/cuisine");
        return ResponseEntity.ok(result);
    }

    // S5-F11: GET /api/payments/analytics/methods?startDate={d}&endDate={d}
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/analytics/methods")
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethodBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Received {} {}", "GET", "/api/payments/analytics/methods");
        List<PaymentMethodDTO> breakdown =
                paymentService.getPaymentMethodBreakdown(startDate, endDate);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/analytics/methods");
        return ResponseEntity.ok(breakdown);
    }

    // [S5-F12] Process Order Refund with Delivery Fee Handling
    @PostMapping("/{id}/refund-with-fee-handling")
    public ResponseEntity<Payment> processOrderRefundWithDeliveryFeeHandling(
            @PathVariable Long id,
            @RequestBody RefundRequest refundRequest) {
        log.info("Received {} {}", "POST", "/api/payments/" + id + "/refund-with-fee-handling");
        ResponseEntity<Payment> response = paymentService.processOrderRefundWithDeliveryFeeHandling(id, refundRequest);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "POST", "/api/payments/" + id + "/refund-with-fee-handling");
        return response;
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<BigDecimal> getUserPaymentTotal(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Received {} {}", "GET", "/api/payments/user/" + userId + "/total");
        BigDecimal total = paymentService.getUserPaymentTotal(userId, startDate, endDate);
        log.info("Returning {} for {} {}", HttpStatus.OK.value(), "GET", "/api/payments/user/" + userId + "/total");
        return ResponseEntity.ok(total);
    }

    private void authorizePaymentRequest(ProcessPaymentRequestDTO dto, Long requesterUserId, String requesterRole) {
        if ("ADMIN".equalsIgnoreCase(requesterRole)) {
            return;
        }

        if (requesterUserId == null || dto == null || dto.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing user identification for authorization");
        }

        if (!requesterUserId.equals(dto.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to process another user's payment");
        }
    }

}
