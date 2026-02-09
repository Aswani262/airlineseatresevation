package com.airline.booking.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Repository
public class BookingQueryJdbcRepository implements BookingQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookingQueryJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BookingSnapshot getSnapshot(UUID bookingId) {
        var params = new MapSqlParameterSource()
                .addValue("bookingId", bookingId);

        // 1) Booking header
        List<BookingHeader> headers = jdbcTemplate.query("""
            SELECT id, booking_reference, flight_id, status, hold_expires_at
            FROM bookings
            WHERE id = :bookingId
        """, params, (rs, rowNum) -> new BookingHeader(
                rs.getObject("id", UUID.class),
                rs.getString("booking_reference"),
                rs.getObject("flight_id", UUID.class),
                rs.getString("status"),
                rs.getObject("hold_expires_at", OffsetDateTime.class)
        ));

        if (headers.isEmpty()) {
            throw new NoSuchElementException("Booking not found: " + bookingId);
        }
        BookingHeader booking = headers.get(0);

        // 2) Seats
        List<BookingSnapshot.SeatLine> seats = jdbcTemplate.query("""
            SELECT passenger_id, seat_number, fare_class
            FROM booking_seats
            WHERE booking_id = :bookingId
        """, params, (rs, rowNum) -> new BookingSnapshot.SeatLine(
                rs.getObject("passenger_id", UUID.class),
                rs.getString("seat_number"),
                rs.getString("fare_class")
        ));

        // 3) Passengers
        List<BookingSnapshot.PassengerLine> passengers = jdbcTemplate.query("""
            SELECT id, first_name, last_name
            FROM passengers
            WHERE booking_id = :bookingId
        """, params, (rs, rowNum) -> new BookingSnapshot.PassengerLine(
                rs.getObject("id", UUID.class),
                rs.getString("first_name"),
                rs.getString("last_name")
        ));

        return new BookingSnapshot(
                booking.id(),
                booking.bookingReference(),
                booking.flightId(),
                booking.status(),
                booking.holdExpiresAt(),
                seats,
                passengers
        );
    }

    // Small internal projection to avoid Object[] casting
    private record BookingHeader(
            UUID id,
            String bookingReference,
            UUID flightId,
            String status,
            OffsetDateTime holdExpiresAt
    ) {}
}
