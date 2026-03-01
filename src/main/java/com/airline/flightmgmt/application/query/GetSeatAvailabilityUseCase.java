package com.airline.flightmgmt.application.query;


import com.airline.flightmgmt.api.dto.SeatAvalibityResponse;

import java.util.List;
import java.util.UUID;

public interface GetSeatAvailabilityUseCase {
    List<SeatAvalibityResponse> getSeatsAvailability(UUID flightId, String fareClass, String status);
}
