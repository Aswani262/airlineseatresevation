package com.airline.payment.service;

import com.airline.payment.application.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PaymentCoreServiceTest {

    private PaymentCoreService service;

    @BeforeEach
    void setup() {
        service = new PaymentCoreService();
    }

    @Test
    void createPendingPayment_shouldCreatePayment_withNormalizedCurrency_andDefaultMethod() {
        UUID bookingId = UUID.randomUUID();

        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(bookingId)
                .amount(new BigDecimal("1500.00"))
                .currency(" inr ")
                .paymentMethod(null)   // default CARD
                .returnUrl("https://app/return")
                .build();

        Payment p = service.createPendingPayment(cmd);

        assertThat(p.getId()).isNotNull();
        assertThat(p.getBookingId()).isEqualTo(bookingId);
        assertThat(p.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(p.getCurrency()).isEqualTo("INR");

        assertThat(p.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(p.getStatus()).isEqualTo("PENDING");

        assertThat(p.getRedirectUrl())
                .startsWith("https://demo-gateway/pay?paymentId=");

        assertThat(p.getReturnUrl()).isEqualTo("https://app/return");
    }

    @Test
    void createPendingPayment_shouldNormalizeProvidedPaymentMethod() {
        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .currency("usd")
                .paymentMethod(" upi ")
                .returnUrl("https://return")
                .build();

        Payment p = service.createPendingPayment(cmd);

        assertThat(p.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(p.getCurrency()).isEqualTo("USD");
    }

    @Test
    void createPendingPayment_shouldGenerateUniqueRedirectUrlPerCall() {
        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .paymentMethod("CARD")
                .returnUrl("x")
                .build();

        Payment p1 = service.createPendingPayment(cmd);
        Payment p2 = service.createPendingPayment(cmd);

        assertThat(p1.getRedirectUrl()).isNotEqualTo(p2.getRedirectUrl());
        assertThat(p1.getId()).isNotEqualTo(p2.getId());
    }

    @Test
    void createPendingPayment_shouldThrow_whenAmountIsZero() {
        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .currency("INR")
                .paymentMethod("CARD")
                .returnUrl("x")
                .build();

        assertThatThrownBy(() -> service.createPendingPayment(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
    }

    @Test
    void createPendingPayment_shouldThrow_whenAmountIsNegative() {
        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(new BigDecimal("-1.00"))
                .currency("INR")
                .paymentMethod("CARD")
                .returnUrl("x")
                .build();

        assertThatThrownBy(() -> service.createPendingPayment(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");
    }
}
