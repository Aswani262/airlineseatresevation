package com.airline.booking.repository;

import com.airline.booking.domain.model.Booking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class BookingJdbcRepository implements BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveBooking(Booking booking) {
        // Insert the booking
        jdbcTemplate.update("""
            INSERT INTO bookings (id, booking_reference, flight_id, customer_id, total_amount, currency, status, hold_expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, booking.getId(), booking.getBookingReference(), booking.getFlightId(), booking.getCustomerId(),
                booking.getTotalAmount(), booking.getCurrency(), booking.getStatus().name(), booking.getHoldExpiresAt());

        //TODO:  consider using batch updates for passengers and seats for
        // better performance when there are many.
        // Insert passengers
        for (var p : booking.getPassengers()) {
            jdbcTemplate.update("""
                INSERT INTO passengers (id, booking_id, first_name, last_name, email, phone, passport_number, passenger_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, p.getId(), p.getBookingId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone(),
                    p.getPassportNumber(), p.getPassengerType().name());
        }

        // Insert booking seats
        for (var s : booking.getSeats()) {
            jdbcTemplate.update("""
                INSERT INTO booking_seats (id, booking_id, passenger_id, seat_number, fare_class, price)
                VALUES (?, ?, ?, ?, ?, ?)
            """, s.getId(), s.getBookingId(), s.getPassengerId(), s.getSeatNumber(), s.getFareClass(), s.getPrice());
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