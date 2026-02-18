package com.airline.flightmgmt.service;

import com.airline.flightmgmt.domain.SeatInventory;
import com.airline.shared.model.SeatLockResult;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface ISeatInventoryService {
    SeatLockResult lockSeats(List<SeatInventory> seats, UUID bookingId, Duration ttl);

    void confirmLockedSeatsOrThrow(List<SeatInventory> seats, UUID bookingId);

    void releaseLockedSeatsOrThrow(List<SeatInventory> seats, UUID bookingId);

    void releaseBookedSeats(List<SeatInventory> seats);

    List<String> normalizeSeats(List<String> seatNumbers);
}
