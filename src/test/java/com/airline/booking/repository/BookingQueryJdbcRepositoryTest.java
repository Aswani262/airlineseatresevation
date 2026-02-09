package com.airline.booking.repository;

import com.airline.booking.repository.BookingQueryRepository.BookingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.groups.Tuple.tuple;

@JdbcTest
@Import(BookingQueryJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class BookingQueryJdbcRepositoryTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    BookingQueryJdbcRepository repo;

    private NamedParameterJdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        jdbc = new NamedParameterJdbcTemplate(dataSource);

        jdbc.getJdbcTemplate().execute("""
            DROP TABLE IF EXISTS booking_seats;
            DROP TABLE IF EXISTS passengers;
            DROP TABLE IF EXISTS bookings;

            CREATE TABLE bookings (
                id UUID PRIMARY KEY,
                booking_reference VARCHAR(30) NOT NULL,
                flight_id UUID NOT NULL,
                status VARCHAR(20) NOT NULL,
                hold_expires_at TIMESTAMP WITH TIME ZONE NULL
            );

            CREATE TABLE booking_seats (
                booking_id UUID NOT NULL,
                passenger_id UUID NOT NULL,
                seat_number VARCHAR(5) NOT NULL,
                fare_class VARCHAR(30) NOT NULL
            );

            CREATE TABLE passengers (
                id UUID PRIMARY KEY,
                booking_id UUID NOT NULL,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL
            );
        """);
    }

    @Test
    void getSnapshot_shouldReturnHeaderSeatsAndPassengers_whenBookingExists() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        OffsetDateTime holdExpiresAt = OffsetDateTime.of(2026, 2, 8, 10, 30, 0, 0, ZoneOffset.UTC);

        jdbc.getJdbcTemplate().update("""
            INSERT INTO bookings (id, booking_reference, flight_id, status, hold_expires_at)
            VALUES (?, ?, ?, ?, ?)
        """, bookingId, "BR-123", flightId, "DRAFT", holdExpiresAt);

        jdbc.getJdbcTemplate().update("""
            INSERT INTO passengers (id, booking_id, first_name, last_name)
            VALUES (?, ?, ?, ?)
        """, p1, bookingId, "John", "Doe");

        jdbc.getJdbcTemplate().update("""
            INSERT INTO passengers (id, booking_id, first_name, last_name)
            VALUES (?, ?, ?, ?)
        """, p2, bookingId, "Jane", "Roe");

        jdbc.getJdbcTemplate().update("""
            INSERT INTO booking_seats (booking_id, passenger_id, seat_number, fare_class)
            VALUES (?, ?, ?, ?)
        """, bookingId, p1, "12A", "ECONOMY");

        jdbc.getJdbcTemplate().update("""
            INSERT INTO booking_seats (booking_id, passenger_id, seat_number, fare_class)
            VALUES (?, ?, ?, ?)
        """, bookingId, p2, "12B", "ECONOMY");

        BookingSnapshot snapshot = repo.getSnapshot(bookingId);

        assertThat(snapshot.bookingId()).isEqualTo(bookingId);
        assertThat(snapshot.bookingReference()).isEqualTo("BR-123");
        assertThat(snapshot.flightId()).isEqualTo(flightId);
        assertThat(snapshot.status()).isEqualTo("DRAFT");
        assertThat(snapshot.holdExpiresAt()).isEqualTo(holdExpiresAt);

        assertThat(snapshot.seats())
                .hasSize(2)
                .extracting(
                        BookingSnapshot.SeatLine::passengerId,
                        BookingSnapshot.SeatLine::seatNumber,
                        BookingSnapshot.SeatLine::fareClass
                )
                .containsExactlyInAnyOrder(
                        tuple(p1, "12A", "ECONOMY"),
                        tuple(p2, "12B", "ECONOMY")
                );

        assertThat(snapshot.passengers())
                .hasSize(2)
                .extracting(
                        BookingSnapshot.PassengerLine::passengerId,
                        BookingSnapshot.PassengerLine::firstName,
                        BookingSnapshot.PassengerLine::lastName
                )
                .containsExactlyInAnyOrder(
                        tuple(p1, "John", "Doe"),
                        tuple(p2, "Jane", "Roe")
                );
    }

    @Test
    void getSnapshot_shouldThrow_whenBookingNotFound() {
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> repo.getSnapshot(missing))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Booking not found:")
                .hasMessageContaining(missing.toString());
    }

    @Test
    void getSnapshot_shouldReturnEmptyLists_whenNoSeatsOrPassengers() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();

        jdbc.getJdbcTemplate().update("""
            INSERT INTO bookings (id, booking_reference, flight_id, status, hold_expires_at)
            VALUES (?, ?, ?, ?, ?)
        """, bookingId, "BR-EMPTY", flightId, "DRAFT", null);

        BookingSnapshot snapshot = repo.getSnapshot(bookingId);

        assertThat(snapshot.seats()).isEmpty();
        assertThat(snapshot.passengers()).isEmpty();
        assertThat(snapshot.holdExpiresAt()).isNull();
    }
}
