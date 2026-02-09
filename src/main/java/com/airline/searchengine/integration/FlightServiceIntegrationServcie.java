package com.airline.searchengine.integration;

import com.airline.flightmgmt.domain.Aircraft;
import com.airline.flightmgmt.domain.Flight;
import com.airline.flightmgmt.domain.Route;
import com.airline.flightmgmt.repository.FlightQueryRepository;

import java.util.UUID;

public interface FlightServiceIntegrationServcie {
    //Replace with dto
    public Flight getFlightById(UUID uuid);

    public Route getRouteById(UUID uuid);

    public Aircraft getAircraftById(UUID uuid);


}
