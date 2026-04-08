package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.dto.UserPaymentSummaryDTO;
import com.team05.fooddelivery.checkout.enums.PaymentStatus;
import com.team05.fooddelivery.checkout.model.Payment;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
}
