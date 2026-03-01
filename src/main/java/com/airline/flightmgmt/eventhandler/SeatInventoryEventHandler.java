package com.airline.flightmgmt.eventhandler;

import com.airline.shared.events.BookingCancelledEvent;
import com.airline.shared.events.BookingConfirmationFailedEvent;
import com.airline.shared.events.BookingFinalizationFailedEvent;
import org.springframework.context.event.EventListener;

public interface SeatInventoryEventHandler {
    @EventListener
    void handle(BookingConfirmationFailedEvent event);

    @EventListener
    void handle(BookingFinalizationFailedEvent event);

    void handle(BookingCancelledEvent event);
}
