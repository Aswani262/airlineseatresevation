package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.Flight;
import com.airline.flightmgmt.domain.FlightCache;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FlightCacheRepository implements IFlightCacheRepository {

    private final IFlightCommandRepository flightCommandRepository;

    @Cacheable(value = "flight", key = "#flightId")
    @Override
    public FlightCache getFlight(UUID flightId) {
        Flight flight =  flightCommandRepository.findById(flightId).get();

        FlightCache flightCache = new FlightCache();
        flightCache.setFlightId(flight.getId());
        flightCache.setFlightDate(flight.getFlightDate());
        flightCache.setAircraftId(flight.getAircraftId());

        return flightCache;

    }

}
