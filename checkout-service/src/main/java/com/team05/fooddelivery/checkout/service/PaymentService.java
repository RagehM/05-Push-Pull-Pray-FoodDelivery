package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
}
