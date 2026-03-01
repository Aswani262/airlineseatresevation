package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.Flight;
import com.airline.flightmgmt.domain.FlightCache;

import java.util.UUID;

public interface IFlightCacheRepository {
    FlightCache getFlight(UUID flightId);

}
