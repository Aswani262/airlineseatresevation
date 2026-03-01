package com.airline.payment.application;

import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import com.airline.payment.domain.PaymentStatus;
import com.airline.payment.repository.IPaymentCommandRepository;
import com.airline.payment.service.DemoGatewaySimulator;
import com.airline.payment.service.PaymentCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiatePaymentHandlerTest {

    @InjectMocks
    private InitiatePaymentHandler initiatePaymentHandler;

    @Mock
    private PaymentCoreService paymentCoreService;

    @Mock
    private IPaymentCommandRepository paymentRepository;

    @Mock
    private DemoGatewaySimulator demoGatewaySimulator;

    private InitiatePaymentCommand command;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        command = InitiatePaymentCommand.builder()
                .bookingId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .returnUrl("https://example.com/return")
                .webhookUrl("https://example.com/webhook")
                .paymentMethod("CREDIT_CARD")
                .build();

        mockPayment = Payment.builder()
                .id(UUID.randomUUID())
                .bookingId(command.getBookingId())
                .amount(command.getAmount())
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .redirectUrl("https://demo-gateway/pay?paymentId=" + UUID.randomUUID())
                .returnUrl(command.getReturnUrl())
                .build();
    }

    @Test
    void initiate_validCommand_returnsInitiatePaymentResult() {
        // Arrange
        when(paymentCoreService.createPendingPayment(command)).thenReturn(mockPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        doNothing().when(demoGatewaySimulator).simulateGatewayCallback(any(UUID.class), any(UUID.class));

        // Act
        InitiatePaymentResult result = initiatePaymentHandler.initiate(command);

        // Assert
        assertNotNull(result);
        assertEquals(mockPayment.getId(), result.paymentId());
        assertEquals(mockPayment.getBookingId(), result.bookingId());

        // Verify interactions
        verify(paymentCoreService).createPendingPayment(command);
        verify(paymentRepository).save(mockPayment);
        verify(demoGatewaySimulator).simulateGatewayCallback(mockPayment.getId(), mockPayment.getBookingId());
    }

    @Test
    void initiate_invalidAmount_throwsIllegalArgumentException() {
        // Arrange
        command.setAmount(BigDecimal.ZERO);
        when(paymentCoreService.createPendingPayment(command)).thenThrow(new IllegalArgumentException("amount must be > 0"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> initiatePaymentHandler.initiate(command));
        assertEquals("amount must be > 0", exception.getMessage());

        verify(paymentCoreService).createPendingPayment(command);
        verifyNoInteractions(paymentRepository, demoGatewaySimulator);
    }
}