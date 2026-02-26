package com.airline.flightmgmt.eventhandler;

import com.airline.flightmgmt.application.command.ReleaseHoldSeatUseCase;
import com.airline.flightmgmt.application.command.dto.ReleasedHoldSeatCommand;
import com.airline.shared.annotation.EventService;
import com.airline.shared.events.ReleaseHoldSeatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

@EventService
@RequiredArgsConstructor
public class DefaultSeatInventoryEventHandler implements SeatInventoryEventHandler {

    private final ReleaseHoldSeatUseCase releaseHoldSeatUseCase;


    @EventListener
    public void handle(ReleaseHoldSeatEvent event){


        releaseHoldSeatUseCase.releaseHoldSeat(new ReleasedHoldSeatCommand(event.getFlightId(),event.getSeatTemplateIds()));
    }
}
