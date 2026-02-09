package com.airline.booking.repository;

import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.domain.model.BookingStatus;
import com.airline.booking.domain.model.FareClassCode;
import com.airline.booking.domain.model.Passenger;
import com.airline.booking.domain.model.PassengerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.airline.flightmgmt.domain.FareClass.BUSINESS;
import static com.airline.flightmgmt.domain.FareClass.ECONOMY;
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
                currency VARCHAR(3) NOT NULL,
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
    void saveBooking_shouldInsertBookingRow() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OffsetDateTime holdExpiresAt = OffsetDateTime.of(2026, 2, 8, 10, 0, 0, 0, ZoneOffset.UTC);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("BR-001")
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("1234.50"))
                .currency("INR")
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(holdExpiresAt)
                .bookingDate(null)
                .passengers(new ArrayList<>())
                .seats(new ArrayList<>())
                .tickets(new ArrayList<>())
                .build();

        repo.saveBooking(booking);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM bookings WHERE id = ?",
                bookingId
        );

        assertThat(row.get("booking_reference")).isEqualTo("BR-001");
        assertThat(row.get("flight_id")).isEqualTo(flightId);
        assertThat(row.get("customer_id")).isEqualTo(customerId);
        assertThat(((BigDecimal) row.get("total_amount"))).isEqualByComparingTo("1234.50");
        assertThat(row.get("currency")).isEqualTo("INR");
        assertThat(row.get("status")).isEqualTo("DRAFT");

        // H2 returns TIMESTAMP WITH TIME ZONE as OffsetDateTime or Timestamp depending on driver/config.
        Object ts = row.get("hold_expires_at");
        assertThat(ts).isNotNull();
    }

    @Test
    void saveBooking_shouldInsertPassengers() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        UUID passengerId1 = UUID.randomUUID();
        UUID passengerId2 = UUID.randomUUID();

        List<Passenger> passengers = new ArrayList<>();

        Passenger p1 = Passenger.builder()
                .id(passengerId1)
                .bookingId(bookingId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("999")
                .passportNumber("P1")
                .passengerType(PassengerType.ADULT)
                .dateOfBirth(null)
                .build();
        passengers.add(p1);

        Passenger p2 = Passenger.builder()
                .id(passengerId2)
                .bookingId(bookingId)
                .firstName("Jane")
                .lastName("Roe")
                .email(null)
                .phone(null)
                .passportNumber(null)
                .passengerType(PassengerType.CHILD)
                .dateOfBirth(null)
                .build();
        passengers.add(p2);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("BR-002")
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("10.00"))
                .currency("INR")
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(null)
                .bookingDate(null)
                .passengers(passengers)
                .seats(new ArrayList<>())
                .tickets(new ArrayList<>())
                .build();

        repo.saveBooking(booking);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM passengers WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        assertThat(count).isEqualTo(2);

        // Verify data for first passenger
        Map<String, Object> row1 = jdbcTemplate.queryForMap(
                "SELECT * FROM passengers WHERE id = ?",
                passengerId1
        );

        assertThat(row1.get("first_name")).isEqualTo("John");
        assertThat(row1.get("last_name")).isEqualTo("Doe");
        assertThat(row1.get("passenger_type")).isEqualTo("ADULT");
    }

    @Test
    void saveBooking_shouldInsertBookingSeats() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        UUID passengerId1 = UUID.randomUUID();
        UUID passengerId2 = UUID.randomUUID();

        List<Passenger> passengers = new ArrayList<>();

        Passenger p1 = Passenger.builder()
                .id(passengerId1)
                .bookingId(bookingId)
                .firstName("A")
                .lastName("B")
                .passengerType(PassengerType.ADULT)
                .build();
        passengers.add(p1);

        Passenger p2 = Passenger.builder()
                .id(passengerId2)
                .bookingId(bookingId)
                .firstName("C")
                .lastName("D")
                .passengerType(PassengerType.CHILD)
                .build();
        passengers.add(p2);

        UUID seatId1 = UUID.randomUUID();
        UUID seatId2 = UUID.randomUUID();

        List<BookingSeat> seats = new ArrayList<>();
        ;
        BookingSeat s1 = BookingSeat.builder()
                .id(seatId1)
                .bookingId(bookingId)
                .passengerId(passengerId1)
                .seatNumber("12A")
                .fareClass(new FareClassCode(ECONOMY.name()))
                .price(new BigDecimal("500.00"))
                .build();
        seats.add(s1);

        BookingSeat s2 = BookingSeat.builder()
                .id(seatId2)
                .bookingId(bookingId)
                .passengerId(passengerId2)
                .seatNumber("14B")
                .fareClass(new FareClassCode(BUSINESS.name()))
                .price(new BigDecimal("700.00"))
                .build();
        seats.add(s2);

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("BR-003")
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("100.00"))
                .currency("INR")
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(null)
                .bookingDate(null)
                .passengers(passengers)
                .seats(seats)
                .tickets(new ArrayList<>())
                .build();

        repo.saveBooking(booking);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM booking_seats WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        assertThat(count).isEqualTo(2);

        // verify seat 1 mapped to passengerId1
        Map<String, Object> rowSeat1 = jdbcTemplate.queryForMap("""
            SELECT * FROM booking_seats
            WHERE booking_id = ? AND seat_number = ?
        """, bookingId, "12A");

        assertThat(rowSeat1.get("passenger_id")).isEqualTo(passengerId1);
        assertThat(rowSeat1.get("fare_class")).isEqualTo("ECONOMY");
        assertThat(((BigDecimal) rowSeat1.get("price"))).isEqualByComparingTo("500.00");

        // verify seat 2 mapped to passengerId2
        Map<String, Object> rowSeat2 = jdbcTemplate.queryForMap("""
            SELECT * FROM booking_seats
            WHERE booking_id = ? AND seat_number = ?
        """, bookingId, "14B");

        assertThat(rowSeat2.get("passenger_id")).isEqualTo(passengerId2);
        assertThat(rowSeat2.get("fare_class")).isEqualTo("BUSINESS");
    }

    @Test
    void updateStatus_shouldReturn1_whenFromStatusMatches_and0Otherwise() {
        UUID bookingId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference("BR-004")
                .flightId(flightId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("1.00"))
                .currency("INR")
                .status(BookingStatus.DRAFT)
                .holdExpiresAt(null)
                .bookingDate(null)
                .passengers(new ArrayList<>())
                .seats(new ArrayList<>())
                .tickets(new ArrayList<>())
                .build();

        repo.saveBooking(booking);

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