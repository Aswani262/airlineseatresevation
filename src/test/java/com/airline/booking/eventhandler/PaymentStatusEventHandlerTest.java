package com.airline.booking.eventhandler;

import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.ConfirmBookingUseCase;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.shared.events.PaymentStatusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentStatusEventHandlerTest {

    @InjectMocks
    private PaymentStatusEventHandler paymentStatusEventHandler;

    @Mock
    private ConfirmBookingUseCase confirmBookingUseCase;

    @Mock
    private CancelBookingUseCase cancelBookingUseCase;

    @Test
    void on_successfulPayment_callsConfirmBooking() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        PaymentStatusEvent event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.SUCCESSFUL, null);

        // Act
        paymentStatusEventHandler.on(event);

        // Assert
        ArgumentCaptor<ConfirmBookingCommand> captor = ArgumentCaptor.forClass(ConfirmBookingCommand.class);
        verify(confirmBookingUseCase).bookingFinalize(captor.capture());
        ConfirmBookingCommand actual = captor.getValue();
        assertEquals(bookingId, actual.getBookingId());
        verifyNoInteractions(cancelBookingUseCase);
    }

    @Test
    void on_failedPayment_callsCancelBooking_withReason() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        String reason = "Insufficient funds";
        PaymentStatusEvent event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.FAILED, reason);

        // Act
        paymentStatusEventHandler.on(event);

        // Assert
        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());
        CancelBookingCommand actual = captor.getValue();
        assertEquals(bookingId, actual.getBookingId());
        assertEquals("FAILED: " + reason, actual.getReason());
        verifyNoInteractions(confirmBookingUseCase);
    }

    @Test
    void on_failedPayment_callsCancelBooking_withoutReason() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        PaymentStatusEvent event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.FAILED, null);

        // Act
        paymentStatusEventHandler.on(event);

        // Assert
        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());
        CancelBookingCommand actual = captor.getValue();
        assertEquals(bookingId, actual.getBookingId());
        assertEquals("FAILED", actual.getReason());
        verifyNoInteractions(confirmBookingUseCase);
    }

    @Test
    void on_expiredPayment_callsCancelBooking_withReason() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        String reason = "Payment timeout";
        PaymentStatusEvent event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.EXPIRED, reason);

        // Act
        paymentStatusEventHandler.on(event);

        // Assert
        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());
        CancelBookingCommand actual = captor.getValue();
        assertEquals(bookingId, actual.getBookingId());
        assertEquals("EXPIRED: " + reason, actual.getReason());
        verifyNoInteractions(confirmBookingUseCase);
    }

    @Test
    void on_expiredPayment_callsCancelBooking_withoutReason() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        PaymentStatusEvent event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.EXPIRED, null);

        // Act
        paymentStatusEventHandler.on(event);

        // Assert
        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());
        CancelBookingCommand actual = captor.getValue();
        assertEquals(bookingId, actual.getBookingId());
        assertEquals("EXPIRED", actual.getReason());
        verifyNoInteractions(confirmBookingUseCase);
    }
}