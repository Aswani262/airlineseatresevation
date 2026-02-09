package com.airline.searchengine.integration;

import com.airline.flightmgmt.domain.Aircraft;
import com.airline.flightmgmt.domain.Flight;
import com.airline.flightmgmt.domain.Route;
import com.airline.shared.annoation.IntegrationService;

import java.util.UUID;

@IntegrationService
public class HttpFlightServiceIntegrationServcie implements FlightServiceIntegrationServcie{

    //private final FlightQueryRepository flightReadRepository;
    //private final RouteReadRepository routeReadRepository;
    //private final AircraftReadRepository aircraftReadRepository;

    //In actual implementation , we will use http client to call the flight service and get the data and map it to our domain model
    @Override
    public Flight getFlightById(UUID uuid) {
        return null;
    }

    @Override
    public Route getRouteById(UUID uuid) {
        return null;
    }

    @Override
    public Aircraft getAircraftById(UUID uuid) {
        return null;
    }
}
