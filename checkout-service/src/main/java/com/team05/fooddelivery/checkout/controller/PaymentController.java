package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    // Payment CRUD
    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {
        return paymentService.createPayment(payment);
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

    @PutMapping("/{id}/refund")
    public Payment refundPayment(@PathVariable Long id, @RequestBody String reason) {
        return paymentService.refundPayment(id, reason);
    }

    // S5-F7: PUT /api/payments/{id}/retry
    @PutMapping("/{id}/retry")
    public ResponseEntity<Payment> retryFailedPayment(@PathVariable Long id) {
        Payment updated = paymentService.retryFailedPayment(id);
        return ResponseEntity.ok(updated);
    }

}
