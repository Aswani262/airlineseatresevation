package com.airline.flightmgmt.repository;


import com.airline.flightmgmt.api.dto.SeatAssignmentsResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SeatAssigmentQueryRepository {

    List<SeatAssignmentsResponse> getAssignedSeats(UUID flightId, LocalDate flightDate);
}
