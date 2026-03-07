package com.airline.booking.eventhandler;

import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.FinalizeBookingUseCase;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.CancelationReason;
import com.airline.booking.application.command.dto.FinalizeBookingCommand;
import com.airline.shared.annotation.EventService;
import com.airline.shared.events.PaymentStatusEvent;
import com.airline.shared.events.SeatHoldingTimeExpired;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

@EventService
@RequiredArgsConstructor
public class PaymentStatusEventHandler {

    private final FinalizeBookingUseCase finalizeBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;


    @EventListener
    public void on(PaymentStatusEvent event) {

        switch (event.getStatus()) {
            case SUCCESS -> finalizeBookingUseCase.bookingFinalize(
                    new FinalizeBookingCommand(
                            event.getBookingId())
            );
            case FAILED, EXPIRED -> cancelBookingUseCase.cancelBooking(new CancelBookingCommand(event.getBookingId(), CancelationReason.PAYMENT_GATEWAY));
        }
    }

    @EventListener
    public void on(SeatHoldingTimeExpired event) {
        cancelBookingUseCase.cancelBooking(new CancelBookingCommand(event.getBookingId(), CancelationReason.SEAT_HOLDING_TIME_OUT));
    }
}
