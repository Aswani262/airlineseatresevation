package com.airline.booking.eventhandler;

import com.airline.booking.application.command.CancelBookingUseCase;
import com.airline.booking.application.command.ConfirmBookingUseCase;
import com.airline.booking.application.command.dto.CancelBookingCommand;
import com.airline.booking.application.command.dto.ConfirmBookingCommand;
import com.airline.shared.annoation.EventService;
import com.airline.shared.events.PaymentStatusEvent;
import org.springframework.context.event.EventListener;

@EventService
public class PaymentStatusEventHandler {

    private final ConfirmBookingUseCase confirmBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    public PaymentStatusEventHandler(ConfirmBookingUseCase confirmBookingUseCase,
                                     CancelBookingUseCase cancelBookingUseCase) {
        this.confirmBookingUseCase = confirmBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
    }

    /**
     * 2) Seat assignment + confirmation -> converts LOCKED -> BOOKED + booking DRAFT -> CONFIRMED + issues tickets
     */
    @EventListener
    public void on(PaymentStatusEvent event) {

        switch (event.getStatus()) {
            case SUCCESS -> confirmBookingUseCase.bookingFinalize(
                    ConfirmBookingCommand.builder()
                            .bookingId(event.getBookingId())
                            .build()
            );

            case FAILED, EXPIRED -> cancelBookingUseCase.cancel(
                    CancelBookingCommand.builder()
                            .bookingId(event.getBookingId())
                            .reason(event.getStatus().name() + (event.getReason() != null ? (": " + event.getReason()) : ""))
                            .build()
            );
        }
    }
}
