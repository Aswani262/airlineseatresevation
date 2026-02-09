package com.airline.booking.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingQueryRepository {
    BookingSnapshot getSnapshot(UUID bookingId);

    record BookingSnapshot(
            UUID bookingId,
            String bookingReference,
            UUID flightId,
            String status,
            OffsetDateTime holdExpiresAt,
            List<SeatLine> seats,
            List<PassengerLine> passengers
    ) {
        public record SeatLine(UUID passengerId, String seatNumber, String fareClass) {}
        public record PassengerLine(UUID passengerId, String firstName, String lastName) {}
    }
}
