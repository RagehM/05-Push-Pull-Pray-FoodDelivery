package com.team05.fooddelivery.checkout.controller;

import com.team05.fooddelivery.checkout.dto.UserPaymentTotalDTO;
import com.team05.fooddelivery.checkout.service.UserPaymentTotalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * [S5-READ-DB] Exposes {@code GET /api/payments/user/{userId}/total?startDate=&endDate=}.
 *
 * Kept as a separate controller from {@code PaymentController} so the new endpoint
 * is independent of the existing CRUD surface and easy to review.
 */
@RestController
@RequestMapping("/api/payments")
public class UserPaymentTotalController {

    private static final Logger log = LoggerFactory.getLogger(UserPaymentTotalController.class);

    private final UserPaymentTotalService userPaymentTotalService;

    public UserPaymentTotalController(UserPaymentTotalService userPaymentTotalService) {
        this.userPaymentTotalService = userPaymentTotalService;
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<UserPaymentTotalDTO> getUserPaymentTotal(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("HTTP GET /api/payments/user/{}/total startDate={} endDate={}", userId, startDate, endDate);
        UserPaymentTotalDTO body = userPaymentTotalService.getUserPaymentTotal(userId, startDate, endDate);
        return ResponseEntity.ok(body);
    }
}
