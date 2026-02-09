package com.airline.booking.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Repository
public class SeatInventoryJdbcRepository implements SeatInventoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SeatInventoryJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int lockSeats(UUID flightId, List<String> seatNumbers, UUID bookingId, Duration ttl) {

        var now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
        var expiresAt = now.plusSeconds(ttl.getSeconds());


        String sql = """
        UPDATE seat_inventory
          SET status = 'LOCKED',
              locked_by_booking_id = :bookingId,
              lock_expires_at = :expiresAt,
              updated_at = :now
        WHERE flight_id = :flightId
          AND seat_number IN (:seatNumbers)
          AND (
                status = 'AVAILABLE'
                OR (status = 'LOCKED' AND lock_expires_at <= :now)
              )
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bookingId", bookingId)
                .addValue("expiresAt", expiresAt)
                .addValue("now", now)
                .addValue("flightId", flightId)
                .addValue("seatNumbers", seatNumbers);

        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int confirmSeats(UUID flightId, List<String> seatNumbers, UUID bookingId) {
        var now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);

        String sql = """
        UPDATE seat_inventory
        SET status = 'BOOKED',
            locked_by_booking_id = NULL,
            lock_expires_at = NULL
        WHERE flight_id = :flightId
          AND seat_number IN (:seatNumbers)
          AND status = 'LOCKED'
          AND locked_by_booking_id = :bookingId
          AND lock_expires_at > :now
    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("flightId", flightId)
                .addValue("seatNumbers", seatNumbers)
                .addValue("bookingId", bookingId)
                .addValue("now", now);

        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int releaseLockedSeats(UUID flightId, List<String> seatNumbers, UUID bookingId) {
        String sql = """
            UPDATE seat_inventory
            SET status = 'AVAILABLE',
                locked_by_booking_id = NULL,
                lock_expires_at = NULL
            WHERE flight_id = :flightId
              AND seat_number IN (:seatNumbers)
              AND status = 'LOCKED'
              AND locked_by_booking_id = :bookingId
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("flightId", flightId)
                .addValue("seatNumbers", seatNumbers)
                .addValue("bookingId", bookingId);

        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int releaseBookedSeats(UUID flightId, List<String> seatNumbers) {
        String sql = """
            UPDATE seat_inventory
            SET status = 'AVAILABLE',
                version = version + 1
            WHERE flight_id = :flightId
              AND seat_number IN (:seatNumbers)
              AND status = 'BOOKED'
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("flightId", flightId)
                .addValue("seatNumbers", seatNumbers);

        return jdbcTemplate.update(sql, params);
    }
}
