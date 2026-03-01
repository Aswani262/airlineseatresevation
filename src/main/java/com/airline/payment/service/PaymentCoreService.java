package com.airline.payment.service;

import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import com.airline.payment.domain.PaymentStatus;
import com.airline.shared.annotation.CoreService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@CoreService
@RequiredArgsConstructor
public class PaymentCoreService implements IPaymentCoreService {


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
}
