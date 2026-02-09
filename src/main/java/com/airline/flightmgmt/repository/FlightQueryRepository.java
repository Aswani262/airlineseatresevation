package com.airline.flightmgmt.repository;


import com.airline.flightmgmt.api.dto.FlightSearchResponse;

import java.time.LocalDate;
import java.util.List;

public interface FlightQueryRepository {
    List<FlightSearchResponse> search(String origin, String destination, LocalDate date);
}
