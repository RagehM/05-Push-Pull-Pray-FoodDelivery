package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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



    // S5-F7: PUT /api/payments/{id}/retry
    @PutMapping("/{id}/retry")
    public ResponseEntity<Payment> retryFailedPayment(@PathVariable Long id) {
        Payment updated = paymentService.retryFailedPayment(id);
        return ResponseEntity.ok(updated);
    }
}
