package com.airline.flightmgmt.application.query;


import com.airline.flightmgmt.api.dto.FlightSearchResponse;

import java.time.LocalDate;
import java.util.List;

public interface SearchFlightsUseCase {
    List<FlightSearchResponse> search(String origin, String destination, LocalDate date);
}
