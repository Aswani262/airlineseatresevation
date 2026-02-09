package com.airline.booking.repository;

import com.airline.booking.application.command.dto.BookSeatCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
public class BookingJdbcRepository implements BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertBooking(UUID bookingId, String bookingReference, UUID flightId, UUID customerId,
                              BigDecimal totalAmount, String currency, String status, OffsetDateTime holdExpiresAt) {
        jdbcTemplate.update("""
            INSERT INTO bookings (id, booking_reference, flight_id, customer_id, total_amount, status, hold_expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, bookingId, bookingReference, flightId, customerId, totalAmount,
                status, holdExpiresAt);
    }

    @Override
    public List<UUID> insertPassengers(UUID bookingId, List<BookSeatCommand.Passenger> passengers) {
        List<UUID> ids = new ArrayList<>(passengers.size());

        for (var p : passengers) {
            UUID passengerId = UUID.randomUUID();
            ids.add(passengerId);

            jdbcTemplate.update("""
                INSERT INTO passengers (id, booking_id, first_name, last_name, email, phone, passport_number, passenger_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, passengerId, bookingId,
                    p.getFirstName().trim(),
                    p.getLastName().trim(),
                    p.getEmail(),
                    p.getPhone(),
                    p.getPassportNumber(),
                    p.getPassengerType().trim().toUpperCase(Locale.ROOT)
            );
        }

        return ids;
    }

    @Override
    public void insertBookingSeats(UUID bookingId, List<BookSeatCommand.SeatSelection> selections, List<UUID> passengerIds) {
        for (var s : selections) {
            UUID bookingSeatId = UUID.randomUUID();
            UUID passengerId = passengerIds.get(s.getPassengerIndex());

            jdbcTemplate.update("""
                INSERT INTO booking_seats (id, booking_id, passenger_id, seat_number, fare_class, price)
                VALUES (?, ?, ?, ?, ?, ?)
            """, bookingSeatId, bookingId, passengerId,
                    s.getSeatNumber().trim().toUpperCase(Locale.ROOT),
                    s.getFareClass().trim().toUpperCase(Locale.ROOT),
                    s.getPrice()
            );
        }
    }

    @Override
    public int updateStatus(UUID bookingId, String fromStatus, String toStatus, OffsetDateTime updatedAt) {
        return jdbcTemplate.update("""
            UPDATE bookings
            SET status = ?, updated_at = ?
            WHERE id = ? AND status = ?
        """, toStatus, updatedAt, bookingId, fromStatus);
    }
}
