package com.airline.shared.events;

public class FlightInformationUpdated extends IntegrationEvent{
    @Override
    public String getEventType() {
        return "flight.update.v1";
    }
}
