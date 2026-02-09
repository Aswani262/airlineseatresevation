package com.airline.flightmgmt.repository;


import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;

import java.util.List;
import java.util.UUID;

public interface SeatInventoryQueryRepository {
    SeatAvailabilitySummaryResponse getSummary(UUID flightId);

    List<SeatResponse> getSeats(UUID flightId, String fareClass, String status);
}
