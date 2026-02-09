package com.airline.booking.eventhandler;

import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.ConfirmBookingUseCase;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.shared.events.PaymentStatusEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentStatusEventHandlerTest {

    private ConfirmBookingUseCase confirmBookingUseCase;
    private CancelBookingUseCase cancelBookingUseCase;

    private PaymentStatusEventHandler handler;

    @BeforeEach
    void setup() {
        confirmBookingUseCase = mock(ConfirmBookingUseCase.class);
        cancelBookingUseCase = mock(CancelBookingUseCase.class);
        handler = new PaymentStatusEventHandler(confirmBookingUseCase, cancelBookingUseCase);
    }

    @Test
    void on_shouldFinalizeBooking_whenPaymentSuccess() {
        UUID bookingId = UUID.randomUUID();
        var event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.SUCCESS, "paid");

        handler.on(event);

        ArgumentCaptor<ConfirmBookingCommand> captor = ArgumentCaptor.forClass(ConfirmBookingCommand.class);
        verify(confirmBookingUseCase).bookingFinalize(captor.capture());

        ConfirmBookingCommand cmd = captor.getValue();
        assertThat(cmd.getBookingId()).isEqualTo(bookingId);
        assertThat(cmd.getPaymentId()).isNull();
        assertThat(cmd.getTransactionId()).isNull();

        verifyNoInteractions(cancelBookingUseCase);
        verifyNoMoreInteractions(confirmBookingUseCase);
    }

    @Test
    void on_shouldCancelBooking_whenPaymentFailed_withReasonIncluded() {
        UUID bookingId = UUID.randomUUID();
        var event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.FAILED, "card_declined");

        handler.on(event);

        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());

        CancelBookingCommand cmd = captor.getValue();
        assertThat(cmd.getBookingId()).isEqualTo(bookingId);
        assertThat(cmd.getReason()).isEqualTo("FAILED: card_declined");

        verifyNoInteractions(confirmBookingUseCase);
        verifyNoMoreInteractions(cancelBookingUseCase);
    }

    @Test
    void on_shouldCancelBooking_whenPaymentExpired_withReasonIncluded() {
        UUID bookingId = UUID.randomUUID();
        var event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.EXPIRED, "timeout");

        handler.on(event);

        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());

        CancelBookingCommand cmd = captor.getValue();
        assertThat(cmd.getBookingId()).isEqualTo(bookingId);
        assertThat(cmd.getReason()).isEqualTo("EXPIRED: timeout");

        verifyNoInteractions(confirmBookingUseCase);
        verifyNoMoreInteractions(cancelBookingUseCase);
    }

    @Test
    void on_shouldCancelBooking_whenReasonIsNull_withoutColonSuffix() {
        UUID bookingId = UUID.randomUUID();
        var event = new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.FAILED, null);

        handler.on(event);

        ArgumentCaptor<CancelBookingCommand> captor = ArgumentCaptor.forClass(CancelBookingCommand.class);
        verify(cancelBookingUseCase).cancel(captor.capture());

        CancelBookingCommand cmd = captor.getValue();
        assertThat(cmd.getBookingId()).isEqualTo(bookingId);
        assertThat(cmd.getReason()).isEqualTo("FAILED");

        verifyNoInteractions(confirmBookingUseCase);
        verifyNoMoreInteractions(cancelBookingUseCase);
    }
}
