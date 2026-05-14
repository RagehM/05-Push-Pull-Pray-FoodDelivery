package com.team05.fooddelivery.checkout.service;

import com.team05.fooddelivery.checkout.dto.UserPaymentTotalDTO;
import com.team05.fooddelivery.checkout.repository.PaymentRepository;
import com.team05.fooddelivery.contracts.dto.UserDTO;
import com.team05.fooddelivery.contracts.feign.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [S5-READ-DB] Service that powers {@code GET /api/payments/user/{userId}/total}.
 *
 * <p>The feature was originally implemented in S5-F3 with a {@code SELECT COUNT(*) FROM users}
 * query against the shared DB. Under M3 the checkout-service uses an isolated
 * postgres ({@code talabatdb-checkout}) which does not contain a {@code users}
 * table at all, so user existence is verified through Feign to user-service.
 */
@Service
public class UserPaymentTotalService {

    private static final Logger log = LoggerFactory.getLogger(UserPaymentTotalService.class);

    private final PaymentRepository paymentRepository;
    private final UserServiceClient userServiceClient;

    public UserPaymentTotalService(PaymentRepository paymentRepository,
                                   UserServiceClient userServiceClient) {
        this.paymentRepository = paymentRepository;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Return the total amount + per-method breakdown of COMPLETED payments for
     * the given user within an optional date range.
     *
     * @param userId    the user whose payments to aggregate; verified via Feign
     * @param startDate inclusive start date (nullable)
     * @param endDate   inclusive end date (nullable)
     * @throws ResponseStatusException 404 if the user is unknown to user-service
     */
    public UserPaymentTotalDTO getUserPaymentTotal(Long userId,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        log.info("S5-READ-DB user payment total requested userId={} startDate={} endDate={}",
                userId, startDate, endDate);

        // 1) Validate the user exists via Feign (NOT via the local DB anymore).
        verifyUserExists(userId);

        // 2) Local aggregation over checkout-postgres.
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay()    : null;
        LocalDateTime end   = (endDate   != null) ? endDate.atTime(23, 59, 59) : null;

        List<Object[]> rows = paymentRepository
                .findCompletedPaymentTotalsByUserAndDateRange(userId, start, end);

        long totalPayments = 0L;
        double totalAmount = 0.0;
        Map<String, Double> breakdown = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String method = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            double sum = toDouble(row[2]);

            totalPayments += count;
            totalAmount   += sum;
            breakdown.merge(method, sum, Double::sum);
        }

        log.info("S5-READ-DB user payment total computed userId={} totalPayments={} totalAmount={} methods={}",
                userId, totalPayments, totalAmount, breakdown.keySet());

        return UserPaymentTotalDTO.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalPayments(totalPayments)
                .totalAmount(totalAmount)
                .methodBreakdown(breakdown)
                .build();
    }

    private void verifyUserExists(Long userId) {
        try {
            UserDTO user = userServiceClient.getUser(userId);
            if (user == null || user.id() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + userId + " not found");
            }
        } catch (ResponseStatusException rse) {
            // Already shaped correctly (e.g. by FeignConfig.ErrorDecoder on 404).
            throw rse;
        } catch (Exception ex) {
            // Anything else (timeout, connection refused, retries exhausted) is a 502.
            log.warn("Feign call to user-service failed userId={} error={}", userId, ex.toString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "user-service unavailable while verifying user " + userId,
                    ex);
        }
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof BigDecimal bd) return bd.doubleValue();
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }
}
