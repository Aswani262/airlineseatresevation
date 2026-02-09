package com.airline.searchengine.application;

import java.util.UUID;

public interface UpdateFlightSearchDoc {
    void handle(UpdateFlightSearchDocCommand command);

    record UpdateFlightSearchDocCommand(UUID flightId) {}
}
