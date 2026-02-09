package com.airline.booking.repository;

import com.airline.booking.application.command.dto.BookSeatCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(BookingJdbcRepository.class)
//TODO: for real project, consider use Testcontainers with a real PostgreSQL instance instead
// of H2 for more accurate testing of SQL and data types.
// H2's compatibility modes are good but not perfect,
// and can lead to false positives/negatives in tests.
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class BookingJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired BookingJdbcRepository repo;

    @BeforeEach
    void setupSchema() {
        //TODO: in real project, use Flyway or similar for this instead of hardcoding DDL here
        //or at least extract to separate SQL file for readability and maintainability.
        jdbcTemplate.execute("""
            DROP TABLE IF EXISTS booking_seats;
            DROP TABLE IF EXISTS passengers;
            DROP TABLE IF EXISTS bookings;

            CREATE TABLE bookings (
                id UUID PRIMARY KEY,
                booking_reference VARCHAR(30) NOT NULL,
                flight_id UUID NOT NULL,
                customer_id UUID NOT NULL,
                total_amount DECIMAL(12,2) NOT NULL,
                status VARCHAR(20) NOT NULL,
                hold_expires_at TIMESTAMP WITH TIME ZONE NULL,
                updated_at TIMESTAMP WITH TIME ZONE NULL
            );

            CREATE TABLE passengers (
                id UUID PRIMARY KEY,
                booking_id UUID NOT NULL,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL,
                email VARCHAR(200),
                phone VARCHAR(50),
                passport_number VARCHAR(50),
                passenger_type VARCHAR(20) NOT NULL
            );

            CREATE TABLE booking_seats (
                id UUID PRIMARY KEY,
                booking_id UUID NOT NULL,
                passenger_id UUID NOT NULL,
                seat_number VARCHAR(10) NOT NULL,
                fare_class VARCHAR(30) NOT NULL,
                price DECIMAL(12,2) NOT NULL
            );
        """);
    }

    @Test
    void insertBooking_shouldInsertRow() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OffsetDateTime holdExpiresAt = OffsetDateTime.of(2026, 2, 8, 10, 0, 0, 0, ZoneOffset.UTC);

        repo.insertBooking(
                bookingId,
                "BR-001",
                flightId,
                customerId,
                new BigDecimal("1234.50"),
                "INR",                 // NOTE: currency not stored by current SQL
                "DRAFT",
                holdExpiresAt
        );

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM bookings WHERE id = ?",
                bookingId
        );

        assertThat(row.get("booking_reference")).isEqualTo("BR-001");
        assertThat(row.get("flight_id")).isEqualTo(flightId);
        assertThat(row.get("customer_id")).isEqualTo(customerId);
        assertThat(((BigDecimal) row.get("total_amount"))).isEqualByComparingTo("1234.50");
        assertThat(row.get("status")).isEqualTo("DRAFT");

        // H2 returns TIMESTAMP WITH TIME ZONE as OffsetDateTime or Timestamp depending on driver/config.
        Object ts = row.get("hold_expires_at");
        assertThat(ts).isNotNull();
    }

    @Test
    void insertPassengers_shouldInsertAndReturnGeneratedIds_andNormalizePassengerType() {
        UUID bookingId = UUID.randomUUID();

        // Need booking row? Not required by your schema here (no FK), but ok either way.
        repo.insertBooking(
                bookingId, "BR-002", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), "INR", "DRAFT", null
        );

        var p1 = new BookSeatCommand.Passenger();
        p1.setFirstName("  John ");
        p1.setLastName(" Doe  ");
        p1.setEmail("john@example.com");
        p1.setPhone("999");
        p1.setPassportNumber("P1");
        p1.setPassengerType(" adult ");

        var p2 = new BookSeatCommand.Passenger();
        p2.setFirstName(" Jane");
        p2.setLastName("Roe ");
        p2.setEmail(null);
        p2.setPhone(null);
        p2.setPassportNumber(null);
        p2.setPassengerType("CHILD");

        List<UUID> ids = repo.insertPassengers(bookingId, List.of(p1, p2));

        assertThat(ids).hasSize(2);
        assertThat(ids.get(0)).isNotNull();
        assertThat(ids.get(1)).isNotNull();
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM passengers WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        assertThat(count).isEqualTo(2);

        // Verify normalization + trimming for names + passenger type uppercasing
        Map<String, Object> row1 = jdbcTemplate.queryForMap(
                "SELECT * FROM passengers WHERE id = ?",
                ids.get(0)
        );

        assertThat(row1.get("first_name")).isEqualTo("John"); // trimmed
        assertThat(row1.get("last_name")).isEqualTo("Doe");   // trimmed
        assertThat(row1.get("passenger_type")).isEqualTo("ADULT"); // uppercased
    }

    @Test
    void insertBookingSeats_shouldInsertRows_andMapPassengerIndexCorrectly_andNormalizeFields() {
        UUID bookingId = UUID.randomUUID();

        repo.insertBooking(
                bookingId, "BR-003", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "INR", "DRAFT", null
        );

        // Prepare two passengers
        var p1 = new BookSeatCommand.Passenger();
        p1.setFirstName("A");
        p1.setLastName("B");
        p1.setPassengerType("ADULT");

        var p2 = new BookSeatCommand.Passenger();
        p2.setFirstName("C");
        p2.setLastName("D");
        p2.setPassengerType("CHILD");

        List<UUID> passengerIds = repo.insertPassengers(bookingId, List.of(p1, p2));

        // selections map passengerIndex -> passengerIds list
        var s1 = new BookSeatCommand.SeatSelection();
        s1.setPassengerIndex(0);
        s1.setSeatNumber(" 12a ");           // will be trimmed + upper
        s1.setFareClass(" economy ");        // will be trimmed + upper
        s1.setPrice(new BigDecimal("500.00"));

        var s2 = new BookSeatCommand.SeatSelection();
        s2.setPassengerIndex(1);
        s2.setSeatNumber("14B");
        s2.setFareClass("BUSINESS");
        s2.setPrice(new BigDecimal("700.00"));

        repo.insertBookingSeats(bookingId, List.of(s1, s2), passengerIds);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM booking_seats WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        assertThat(count).isEqualTo(2);

        // verify seat 1 mapped to passengerIds[0]
        Map<String, Object> rowSeat1 = jdbcTemplate.queryForMap("""
            SELECT * FROM booking_seats
            WHERE booking_id = ? AND seat_number = ?
        """, bookingId, "12A");

        assertThat(rowSeat1.get("passenger_id")).isEqualTo(passengerIds.get(0));
        assertThat(rowSeat1.get("fare_class")).isEqualTo("ECONOMY");
        assertThat(((BigDecimal) rowSeat1.get("price"))).isEqualByComparingTo("500.00");

        // verify seat 2 mapped to passengerIds[1]
        Map<String, Object> rowSeat2 = jdbcTemplate.queryForMap("""
            SELECT * FROM booking_seats
            WHERE booking_id = ? AND seat_number = ?
        """, bookingId, "14B");

        assertThat(rowSeat2.get("passenger_id")).isEqualTo(passengerIds.get(1));
        assertThat(rowSeat2.get("fare_class")).isEqualTo("BUSINESS");
    }

    @Test
    void updateStatus_shouldReturn1_whenFromStatusMatches_and0Otherwise() {
        UUID bookingId = UUID.randomUUID();

        repo.insertBooking(
                bookingId, "BR-004", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1.00"), "INR", "DRAFT", null
        );

        OffsetDateTime t1 = OffsetDateTime.of(2026, 2, 8, 11, 0, 0, 0, ZoneOffset.UTC);

        int updated = repo.updateStatus(bookingId, "DRAFT", "CONFIRMED", t1);
        assertThat(updated).isEqualTo(1);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM bookings WHERE id = ?",
                String.class,
                bookingId
        );
        assertThat(status).isEqualTo("CONFIRMED");

        // wrong fromStatus => should not update
        OffsetDateTime t2 = OffsetDateTime.of(2026, 2, 8, 12, 0, 0, 0, ZoneOffset.UTC);
        int updated2 = repo.updateStatus(bookingId, "DRAFT", "CANCELLED", t2);
        assertThat(updated2).isEqualTo(0);

        String status2 = jdbcTemplate.queryForObject(
                "SELECT status FROM bookings WHERE id = ?",
                String.class,
                bookingId
        );
        assertThat(status2).isEqualTo("CONFIRMED");
    }
}
