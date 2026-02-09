package com.airline.flightmgmt.application.query;


import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;

import java.util.List;
import java.util.UUID;

public interface GetSeatAvailabilityUseCase {
    SeatAvailabilitySummaryResponse getSummary(UUID flightId);

    List<SeatResponse> getSeats(UUID flightId, String fareClass, String status);

}
