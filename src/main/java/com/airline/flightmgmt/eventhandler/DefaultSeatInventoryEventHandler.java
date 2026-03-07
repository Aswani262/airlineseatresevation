package com.airline.flightmgmt.eventhandler;

import com.airline.flightmgmt.application.command.ReleaseBookedSeatUseCase;
import com.airline.flightmgmt.application.command.ReleaseHoldSeatUseCase;
import com.airline.flightmgmt.application.command.dto.ReleaseBookedSeatCommand;
import com.airline.flightmgmt.application.command.dto.ReleasedHoldSeatCommand;
import com.airline.shared.annotation.EventService;
import com.airline.shared.events.BookingCancelledEvent;
import com.airline.shared.events.BookingConfirmationFailedEvent;
import com.airline.shared.events.BookingFinalizationFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

@EventService
@RequiredArgsConstructor
public class DefaultSeatInventoryEventHandler implements SeatInventoryEventHandler {

    private final ReleaseHoldSeatUseCase releaseHoldSeatUseCase;
    private final ReleaseBookedSeatUseCase releaseBookedSeatUseCase;


    //Release seat which are in hold stage of seat selection
    @EventListener
    @Override
    public void handle(BookingConfirmationFailedEvent event){
        releaseHoldSeatUseCase.releaseHoldSeat(new ReleasedHoldSeatCommand(event.getFlightId(),event.getSeatTemplateIds(),event.getCustomerId()));
    }

    //Released the seat which in Hold stage of Payment
    @Override
    public void handle(BookingFinalizationFailedEvent event) {
        releaseBookedSeatUseCase.releaseBookedSeats(new ReleaseBookedSeatCommand(event.getFlightId(),null,null,event.getBookingId()));
    }

    @Override
    public void handle(BookingCancelledEvent event) {
        releaseBookedSeatUseCase.releaseBookedSeats(new ReleaseBookedSeatCommand(event.getFlightId(),null,null,event.getBookingId()));
    }
}
