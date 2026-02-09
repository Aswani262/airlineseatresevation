package com.airline.payment.application;

import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import com.airline.payment.repository.PaymentRepository;
import com.airline.payment.service.DemoGatewaySimulator;
import com.airline.payment.service.PaymentCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitiatePaymentHandlerTest {

    private PaymentCoreService paymentCoreService;
    private PaymentRepository paymentRepository;
    private DemoGatewaySimulator demoGatewaySimulator;

    private InitiatePaymentHandler handler;

    @BeforeEach
    void setup() {
        paymentCoreService = mock(PaymentCoreService.class);
        paymentRepository = mock(PaymentRepository.class);
        demoGatewaySimulator = mock(DemoGatewaySimulator.class);

        handler = new InitiatePaymentHandler(paymentCoreService, paymentRepository, demoGatewaySimulator);
    }

    @Test
    void initiate_shouldCreatePersistSimulateAndReturnResult() {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 2, 8, 10, 0, 0, 0, ZoneOffset.UTC);

        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(bookingId)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .returnUrl("https://app/return")
                .webhookUrl("https://app/webhook") // not used in handler but part of cmd
                .paymentMethod("CARD")
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.CARD)
                .status("PENDING")
                .redirectUrl("https://demo-gateway/pay?paymentId=" + paymentId)
                .returnUrl("https://app/return")
                .build();

        when(paymentCoreService.createPendingPayment(cmd)).thenReturn(payment);

        InitiatePaymentResult result = handler.initiate(cmd);

        // 1) created in core service
        verify(paymentCoreService).createPendingPayment(cmd);

        // 2) persisted
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).insert(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue()).isSameAs(payment);

        // 3) gateway callback simulation triggered
        verify(demoGatewaySimulator).simulateGatewayCallback(paymentId, bookingId);

        // 4) result fields
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.gateway()).isEqualTo("DEMO_GATEWAY");
        assertThat(result.redirectUrl()).isEqualTo(payment.getRedirectUrl());

        Map<String, String> params = result.gatewayParams();
        assertThat(params).containsEntry("paymentId", paymentId.toString());
        assertThat(params).containsEntry("amount", "1500.00");
        assertThat(params).containsEntry("currency", "INR");
        assertThat(params).containsEntry("returnUrl", "https://app/return");

        verifyNoMoreInteractions(paymentCoreService, paymentRepository, demoGatewaySimulator);
    }

    @Test
    void initiate_shouldPropagateException_whenCoreServiceThrows() {
        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(new BigDecimal("0.00"))
                .currency("INR")
                .returnUrl("x")
                .webhookUrl("y")
                .build();

        when(paymentCoreService.createPendingPayment(cmd))
                .thenThrow(new IllegalArgumentException("amount must be > 0"));

        assertThatThrownBy(() -> handler.initiate(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be > 0");

        verify(paymentCoreService).createPendingPayment(cmd);
        verifyNoInteractions(paymentRepository, demoGatewaySimulator);
    }

    @Test
    void initiate_shouldNotCallGatewaySimulator_whenInsertFails() {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        InitiatePaymentCommand cmd = InitiatePaymentCommand.builder()
                .bookingId(bookingId)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .returnUrl("https://return")
                .webhookUrl("https://webhook")
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status("PENDING")
                .redirectUrl("https://demo-gateway/pay?paymentId=" + paymentId)
                .returnUrl("https://return")
                .build();

        when(paymentCoreService.createPendingPayment(cmd)).thenReturn(payment);
        doThrow(new RuntimeException("db down")).when(paymentRepository).insert(payment);

        assertThatThrownBy(() -> handler.initiate(cmd))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        verify(paymentCoreService).createPendingPayment(cmd);
        verify(paymentRepository).insert(payment);

        // important: should NOT simulate callback if insert failed
        verifyNoInteractions(demoGatewaySimulator);
    }
}
