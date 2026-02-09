package com.airline.booking.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface ISeatInventoryService {
    SeatInventoryService.SeatLockResult lockSeats(UUID flightId, UUID bookingId, List<String> seatNumbers, Duration ttl);

    void ensureLockedOrThrow(SeatInventoryService.SeatLockResult result);

    void confirmLockedSeatsOrThrow(UUID flightId, UUID bookingId, List<String> seatNumbers);

    void releaseLockedSeatsOrThrow(UUID flightId, UUID bookingId, List<String> seatNumbers);

    void releaseBookedSeats(UUID flightId, List<String> seatNumbers);
}
