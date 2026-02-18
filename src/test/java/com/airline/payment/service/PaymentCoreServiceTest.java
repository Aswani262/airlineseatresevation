package com.airline.payment.service;

import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import com.airline.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentCoreServiceTest {

    private PaymentCoreService paymentCoreService;

    private InitiatePaymentCommand validCommand;

    @BeforeEach
    void setUp() {
        paymentCoreService = new PaymentCoreService();

        validCommand = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100.00))
                .currency("usd")
                .returnUrl("https://example.com/return")
                .webhookUrl("https://example.com/webhook")
                .paymentMethod("credit_card")
                .build();
    }

    @Test
    void createPendingPayment_validCommand_returnsPendingPayment() {
        // Act
        Payment payment = paymentCoreService.createPendingPayment(validCommand);

        // Assert
        assertNotNull(payment);
        assertNotNull(payment.getId());
        assertEquals(validCommand.getBookingId(), payment.getBookingId());
        assertEquals(validCommand.getAmount(), payment.getAmount());
        assertEquals("USD", payment.getCurrency());
        assertEquals(PaymentMethod.CREDIT_CARD, payment.getPaymentMethod());
        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
        assertNull(payment.getTransactionId());
    }



    @Test
    void createPendingPayment_normalizesCurrencyAndMethod() {
        // Arrange
        validCommand.setCurrency(" inr ");
        validCommand.setPaymentMethod(" debit_card ");

        // Act
        Payment payment = paymentCoreService.createPendingPayment(validCommand);

        // Assert
        assertEquals("INR", payment.getCurrency());
        assertEquals(PaymentMethod.DEBIT_CARD, payment.getPaymentMethod());
    }

    @Test
    void createPendingPayment_invalidAmount_throwsIllegalArgumentException() {
        // Arrange
        validCommand.setAmount(BigDecimal.ZERO);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentCoreService.createPendingPayment(validCommand));
        assertEquals("amount must be > 0", exception.getMessage());
    }

    @Test
    void createPendingPayment_negativeAmount_throwsIllegalArgumentException() {
        // Arrange
        validCommand.setAmount(BigDecimal.valueOf(-10));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentCoreService.createPendingPayment(validCommand));
        assertEquals("amount must be > 0", exception.getMessage());
    }

    @Test
    void createPendingPayment_invalidPaymentMethod_throwsIllegalArgumentException() {
        // Arrange
        validCommand.setPaymentMethod("INVALID");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> paymentCoreService.createPendingPayment(validCommand));
    }
}