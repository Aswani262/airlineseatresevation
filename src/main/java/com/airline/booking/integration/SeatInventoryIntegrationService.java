package com.airline.booking.integration;

import com.airline.shared.model.SeatLockResult;

import java.util.List;
import java.util.UUID;

public interface SeatInventoryIntegrationService {
    SeatLockResult lockSeats(UUID flightId, List<String> seats, UUID bookingId, int holdMinutes);
}
