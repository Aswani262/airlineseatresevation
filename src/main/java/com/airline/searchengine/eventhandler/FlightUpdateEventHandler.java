package com.airline.searchengine.eventhandler;

import com.airline.shared.annotation.EventService;
import com.airline.shared.events.FlightInformationUpdated;
import org.springframework.context.event.EventListener;

@EventService
public class FlightUpdateEventHandler {

    @EventListener
    public void handleFlightUpdateEvent(FlightInformationUpdated event) {
        // Handle the flight update event, e.g., update the search index
        System.out.println("Received Flight Update Event: " + event);
    }
}
