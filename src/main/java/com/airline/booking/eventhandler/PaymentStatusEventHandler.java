package com.airline.booking.eventhandler;

import com.airline.booking.application.command.CancelConfirmedBookingUseCase;
import com.airline.booking.application.command.CancelPendingBookingUseCase;
import com.airline.booking.application.command.FinalizeBookingUseCase;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelPendingBookingCommand;
import com.airline.booking.application.command.dto.FinalizeBookingCommand;
import com.airline.shared.annotation.EventService;
import com.airline.shared.events.PaymentStatusEvent;
import com.airline.shared.events.PaymentTimeExpired;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

@EventService
@RequiredArgsConstructor
public class PaymentStatusEventHandler {

    private final FinalizeBookingUseCase finalizeBookingUseCase;
    private final CancelConfirmedBookingUseCase cancelConfirmedBookingUseCase;
    private final CancelPendingBookingUseCase cancelPendingBookingUseCase;


    @EventListener
    public void on(PaymentStatusEvent event) {

        switch (event.getStatus()) {
            case SUCCESS -> finalizeBookingUseCase.bookingFinalize(
                    new FinalizeBookingCommand(
                            event.getBookingId())
            );

            case FAILED, EXPIRED -> cancelConfirmedBookingUseCase.cancelConfirmedBooking(new CancelBookingCommand(event.getBookingId(), event.getReason()));
        }
    }

    @EventListener
    public void on(PaymentTimeExpired event) {
        cancelPendingBookingUseCase.cancelPendingBooking(new CancelPendingBookingCommand(event.getBookingId(),"PAYMENT_EXPIRED"));
        }
}
