package com.airline.booking.repository;

import com.airline.booking.application.command.dto.BookSeatCommand;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository {

    void insertBooking(UUID bookingId,
                       String bookingReference,
                       UUID flightId,
                       UUID customerId,
                       BigDecimal totalAmount,
                       String currency,
                       String status,
                       OffsetDateTime holdExpiresAt);

    int updateStatus(UUID bookingId, String fromStatus, String toStatus, OffsetDateTime updatedAt);

    List<UUID> insertPassengers(UUID bookingId, List<BookSeatCommand.Passenger> passengers);

    void insertBookingSeats(UUID bookingId, List<BookSeatCommand.SeatSelection> selections, List<UUID> passengerIds);
}
