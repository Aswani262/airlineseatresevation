package com.airline.booking.service.core;

import com.airline.booking.domain.model.SeatInventory;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface ISeatInventoryService {
    SeatInventoryCoreService.SeatLockResult lockSeats(List<SeatInventory> seats, UUID bookingId, Duration ttl);

    void confirmLockedSeatsOrThrow(List<SeatInventory> seats, UUID bookingId);

    void releaseLockedSeatsOrThrow(List<SeatInventory> seats, UUID bookingId);

    void releaseBookedSeats(List<SeatInventory> seats);

    List<String> normalizeSeats(List<String> seatNumbers);
}
