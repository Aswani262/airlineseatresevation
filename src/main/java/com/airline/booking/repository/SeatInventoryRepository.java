package com.airline.booking.repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface SeatInventoryRepository {
    int lockSeats(UUID flightId, List<String> seatNumbers, UUID bookingId, Duration ttl);
    int confirmSeats(UUID flightId, List<String> seatNumbers, UUID bookingId);
    int releaseLockedSeats(UUID flightId, List<String> seatNumbers, UUID bookingId);
    int releaseBookedSeats(UUID flightId, List<String> seatNumbers);

}

