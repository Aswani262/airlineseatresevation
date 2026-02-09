package com.airline.booking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(SeatInventoryJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class SeatInventoryJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired SeatInventoryJdbcRepository repo;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("""
            DROP TABLE IF EXISTS seat_inventory;

            CREATE TABLE seat_inventory (
                id UUID PRIMARY KEY,
                flight_id UUID NOT NULL,
                seat_number VARCHAR(10) NOT NULL,
                fare_class VARCHAR(30) NOT NULL,
                status VARCHAR(20) NOT NULL,
                price DECIMAL(12,2) NOT NULL,

                locked_by_booking_id UUID NULL,
                lock_expires_at TIMESTAMP WITH TIME ZONE NULL,
                updated_at TIMESTAMP WITH TIME ZONE NULL,

                version INT NOT NULL DEFAULT 0
            );
        """);
    }

    private void insertSeat(UUID flightId, String seatNumber, String status,
                            UUID lockedBy, OffsetDateTime expiresAt, int version) {
        jdbcTemplate.update("""
            INSERT INTO seat_inventory
              (id, flight_id, seat_number, fare_class, status, price, locked_by_booking_id, lock_expires_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                UUID.randomUUID(),
                flightId,
                seatNumber,
                "ECONOMY",
                status,
                new BigDecimal("1000.00"),
                lockedBy,
                expiresAt,
                OffsetDateTime.now(ZoneOffset.UTC),
                version
        );
    }

    @Test
    void lockSeats_shouldLockOnlyAvailableSeats() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        insertSeat(flightId, "1A", "AVAILABLE", null, null, 0);
        insertSeat(flightId, "1B", "BOOKED", null, null, 0);

        int updated = repo.lockSeats(flightId, List.of("1A", "1B"), bookingId, Duration.ofMinutes(10));
        assertThat(updated).isEqualTo(1);

        String status1A = jdbcTemplate.queryForObject("""
            SELECT status FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, String.class, flightId, "1A");

        assertThat(status1A).isEqualTo("LOCKED");

        UUID lockedBy = jdbcTemplate.queryForObject("""
            SELECT locked_by_booking_id FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, UUID.class, flightId, "1A");

        assertThat(lockedBy).isEqualTo(bookingId);
    }

    @Test
    void lockSeats_shouldRelockExpiredLockedSeats() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        // locked but expired
        OffsetDateTime expired = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5);
        insertSeat(flightId, "2A", "LOCKED", UUID.randomUUID(), expired, 0);

        int updated = repo.lockSeats(flightId, List.of("2A"), bookingId, Duration.ofMinutes(10));
        assertThat(updated).isEqualTo(1);

        UUID lockedBy = jdbcTemplate.queryForObject("""
            SELECT locked_by_booking_id FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, UUID.class, flightId, "2A");

        assertThat(lockedBy).isEqualTo(bookingId);

        OffsetDateTime newExpiry = jdbcTemplate.queryForObject("""
            SELECT lock_expires_at FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, OffsetDateTime.class, flightId, "2A");

        assertThat(newExpiry).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void lockSeats_shouldNotLockActiveLockedSeats() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        OffsetDateTime notExpired = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        insertSeat(flightId, "3A", "LOCKED", UUID.randomUUID(), notExpired, 0);

        int updated = repo.lockSeats(flightId, List.of("3A"), bookingId, Duration.ofMinutes(10));
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void confirmSeats_shouldConfirmOnlyIfLockedBySameBooking_andNotExpired() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        OffsetDateTime notExpired = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        insertSeat(flightId, "4A", "LOCKED", bookingId, notExpired, 0);

        int updated = repo.confirmSeats(flightId, List.of("4A"), bookingId);
        assertThat(updated).isEqualTo(1);

        var row = jdbcTemplate.queryForMap("""
            SELECT status, locked_by_booking_id, lock_expires_at
            FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, flightId, "4A");

        assertThat(row.get("status")).isEqualTo("BOOKED");
        assertThat(row.get("locked_by_booking_id")).isNull();
        assertThat(row.get("lock_expires_at")).isNull();
    }

    @Test
    void confirmSeats_shouldNotConfirmIfLockedByDifferentBooking() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID otherBooking = UUID.randomUUID();

        OffsetDateTime notExpired = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        insertSeat(flightId, "5A", "LOCKED", otherBooking, notExpired, 0);

        int updated = repo.confirmSeats(flightId, List.of("5A"), bookingId);
        assertThat(updated).isEqualTo(0);

        String status = jdbcTemplate.queryForObject("""
            SELECT status FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, String.class, flightId, "5A");

        assertThat(status).isEqualTo("LOCKED");
    }

    @Test
    void releaseLockedSeats_shouldReleaseOnlySeatsLockedByThisBooking() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID otherBooking = UUID.randomUUID();

        insertSeat(flightId, "6A", "LOCKED", bookingId, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5), 0);
        insertSeat(flightId, "6B", "LOCKED", otherBooking, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5), 0);

        int updated = repo.releaseLockedSeats(flightId, List.of("6A", "6B"), bookingId);
        assertThat(updated).isEqualTo(1);

        String s6a = jdbcTemplate.queryForObject("""
            SELECT status FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, String.class, flightId, "6A");
        assertThat(s6a).isEqualTo("AVAILABLE");

        String s6b = jdbcTemplate.queryForObject("""
            SELECT status FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, String.class, flightId, "6B");
        assertThat(s6b).isEqualTo("LOCKED");
    }

    @Test
    void releaseBookedSeats_shouldSetAvailable_andIncrementVersion_onlyForBooked() {
        UUID flightId = UUID.randomUUID();

        insertSeat(flightId, "7A", "BOOKED", null, null, 10);
        insertSeat(flightId, "7B", "AVAILABLE", null, null, 20);

        int updated = repo.releaseBookedSeats(flightId, List.of("7A", "7B"));
        assertThat(updated).isEqualTo(1);

        var row = jdbcTemplate.queryForMap("""
            SELECT status, version FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, flightId, "7A");

        assertThat(row.get("status")).isEqualTo("AVAILABLE");
        assertThat(((Number) row.get("version")).intValue()).isEqualTo(11);

        int v7b = jdbcTemplate.queryForObject("""
            SELECT version FROM seat_inventory WHERE flight_id=? AND seat_number=?
        """, Integer.class, flightId, "7B");
        assertThat(v7b).isEqualTo(20);
    }
}
