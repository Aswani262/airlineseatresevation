package com.airline.payment.service;

import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.domain.*;
import com.airline.payment.repository.IPaymentCommandRepository;
import com.airline.shared.annotation.CoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@CoreService
@RequiredArgsConstructor
public class PaymentCoreService implements IPaymentCoreService {

    private final IPaymentCommandRepository paymentRepository;

    /**
     * Idempotent payment creation
     * - SUCCESS or PENDING → return existing
     * - FAILED → create new pending payment
     * - No record → create new
     */
    public Payment createOrReturnExistingPayment(InitiatePaymentCommand cmd) {
        validate(cmd);

        // Step 1: Look for SUCCESS or PENDING payment only
        Optional<Payment> activePayment = paymentRepository
                .findActivePayment(cmd.getBookingId(), List.of(PaymentStatus.SUCCESS, PaymentStatus.PENDING));

        if (activePayment.isPresent()) {
            Payment payment = activePayment.get();
            log.info("Idempotent: Returning existing payment (status={}) for bookingId={}", 
                    payment.getStatus(), cmd.getBookingId());
            return payment;
        }

        // Step 2: If only FAILED payment exists → create new for retry
        Optional<Payment> failedPayment = paymentRepository
                .findByBookingIdAndStatus(cmd.getBookingId(), PaymentStatus.FAILED);

        if (failedPayment.isPresent()) {
            log.info("Previous payment FAILED. Creating new pending payment for retry. bookingId={}", cmd.getBookingId());
        }

        // Step 3: Create new payment
        log.info("Creating new payment for bookingId={}", cmd.getBookingId());
        return createPendingPayment(cmd);
    }

    @Override
    public Payment createPendingPayment(InitiatePaymentCommand cmd) {
        validate(cmd);

        UUID paymentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        String currency = cmd.getCurrency().trim().toUpperCase(Locale.ROOT);
        String method = (cmd.getPaymentMethod() == null || cmd.getPaymentMethod().isBlank())
                ? "CARD"
                : cmd.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        String redirectUrl = "https://demo-gateway/pay?paymentId=" + paymentId;

        return Payment.builder()
                .id(paymentId)
                .bookingId(cmd.getBookingId())
                .amount(cmd.getAmount())
                .currency(currency)
                .paymentMethod(PaymentMethod.valueOf(method))
                .status(PaymentStatus.PENDING)
                .redirectUrl(redirectUrl)
                .returnUrl(cmd.getReturnUrl())
                .build();
    }

    private void validate(InitiatePaymentCommand cmd) {
        if (cmd.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

    public Payment createRefundPayment(Payment originalPayment, String transactionId, String gatewayRawPayload) {

        return Payment.builder()
                .id(UUID.randomUUID())
                .bookingId(originalPayment.getBookingId())           // Same bookingId
                .amount(originalPayment.getAmount())
                .currency(originalPayment.getCurrency())
                .paymentMethod(originalPayment.getPaymentMethod())
                .status(PaymentStatus.PENDING)                       // Start as PENDING for refund processing
                .tranascationFor(TranascationFor.REFUND)
                .reason(Reason.PAYMENT_TIME_OUT)
                .transactionId(transactionId)
                .gatewayResponse(Map.of(
                        "originalPaymentId", originalPayment.getId().toString(),
                        "note", "Late SUCCESS after timeout expiry"
                ))
                .build();
    }

}