package com.airline.flighmngmt.repository;

import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import com.airline.flightmgmt.repository.SeatInventoryQueryJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(SeatInventoryQueryJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class SeatInventoryQueryJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired SeatInventoryQueryJdbcRepository repo;

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
                price DECIMAL(12,2) NOT NULL
            );
        """);
    }

    @Test
    void getSummary_shouldReturnCountsAndMinPricePerFareClass() {
        UUID flightId = UUID.randomUUID();
        UUID otherFlight = UUID.randomUUID();

        // ECONOMY: 2 AVAILABLE (min=200), 1 LOCKED, 1 BOOKED
        insertSeat(UUID.randomUUID(), flightId, "12A", "ECONOMY", "AVAILABLE", "300.00");
        insertSeat(UUID.randomUUID(), flightId, "12B", "ECONOMY", "AVAILABLE", "200.00");
        insertSeat(UUID.randomUUID(), flightId, "12C", "ECONOMY", "LOCKED",    "250.00");
        insertSeat(UUID.randomUUID(), flightId, "12D", "ECONOMY", "BOOKED",    "400.00");

        // BUSINESS: 0 AVAILABLE (min_price should be NULL), 1 LOCKED, 1 BOOKED
        insertSeat(UUID.randomUUID(), flightId, "1A", "BUSINESS", "LOCKED", "1500.00");
        insertSeat(UUID.randomUUID(), flightId, "1B", "BUSINESS", "BOOKED", "1600.00");

        // another flight (should not be counted)
        insertSeat(UUID.randomUUID(), otherFlight, "99Z", "ECONOMY", "AVAILABLE", "1.00");

        SeatAvailabilitySummaryResponse res = repo.getSummary(flightId);

        assertThat(res).isNotNull();
        assertThat(res.getFlightId()).isEqualTo(flightId);
        assertThat(res.getAvailability()).hasSize(2);

        // ordered by fare_class => BUSINESS then ECONOMY (lexicographic)
        SeatAvailabilitySummaryResponse.FareClassAvailability business = res.getAvailability().get(0);
        SeatAvailabilitySummaryResponse.FareClassAvailability economy  = res.getAvailability().get(1);

        assertThat(business.getFareClass()).isEqualTo("BUSINESS");
        assertThat(business.getAvailable()).isEqualTo(0);
        assertThat(business.getLocked()).isEqualTo(1);
        assertThat(business.getBooked()).isEqualTo(1);
        assertThat(business.getMinPrice()).isNull();

        assertThat(economy.getFareClass()).isEqualTo("ECONOMY");
        assertThat(economy.getAvailable()).isEqualTo(2);
        assertThat(economy.getLocked()).isEqualTo(1);
        assertThat(economy.getBooked()).isEqualTo(1);
        assertThat(economy.getMinPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void getSeats_shouldReturnAllSeats_whenFareClassAndStatusAreNull_sorted() {
        UUID flightId = UUID.randomUUID();

        // Insert out-of-order to ensure ORDER BY works (fare_class, seat_number)
        UUID s2 = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s3 = UUID.randomUUID();

        insertSeat(s2, flightId, "12B", "ECONOMY", "AVAILABLE", "200.00");
        insertSeat(s1, flightId, "1A",  "BUSINESS","LOCKED",    "1500.00");
        insertSeat(s3, flightId, "12A", "ECONOMY", "BOOKED",    "300.00");

        List<SeatResponse> seats = repo.getSeats(flightId, null, null);

        assertThat(seats).hasSize(3);

        // BUSINESS first, then ECONOMY; within ECONOMY 12A then 12B
        assertThat(seats.get(0).getSeatId()).isEqualTo(s1);
        assertThat(seats.get(0).getFareClass()).isEqualTo("BUSINESS");
        assertThat(seats.get(0).getSeatNumber()).isEqualTo("1A");

        assertThat(seats.get(1).getSeatId()).isEqualTo(s3);
        assertThat(seats.get(1).getFareClass()).isEqualTo("ECONOMY");
        assertThat(seats.get(1).getSeatNumber()).isEqualTo("12A");

        assertThat(seats.get(2).getSeatId()).isEqualTo(s2);
        assertThat(seats.get(2).getFareClass()).isEqualTo("ECONOMY");
        assertThat(seats.get(2).getSeatNumber()).isEqualTo("12B");
    }

    @Test
    void getSeats_shouldFilterByFareClassOnly() {
        UUID flightId = UUID.randomUUID();

        insertSeat(UUID.randomUUID(), flightId, "1A",  "BUSINESS","AVAILABLE", "1500.00");
        insertSeat(UUID.randomUUID(), flightId, "12A", "ECONOMY", "AVAILABLE", "300.00");
        insertSeat(UUID.randomUUID(), flightId, "12B", "ECONOMY", "BOOKED",   "400.00");

        List<SeatResponse> seats = repo.getSeats(flightId, "ECONOMY", null);

        assertThat(seats).hasSize(2);
        assertThat(seats).allSatisfy(s -> assertThat(s.getFareClass()).isEqualTo("ECONOMY"));
    }

    @Test
    void getSeats_shouldFilterByStatusOnly() {
        UUID flightId = UUID.randomUUID();

        insertSeat(UUID.randomUUID(), flightId, "1A",  "BUSINESS","LOCKED",   "1500.00");
        insertSeat(UUID.randomUUID(), flightId, "12A", "ECONOMY", "AVAILABLE","300.00");
        insertSeat(UUID.randomUUID(), flightId, "12B", "ECONOMY", "LOCKED",   "400.00");

        List<SeatResponse> seats = repo.getSeats(flightId, null, "LOCKED");

        assertThat(seats).hasSize(2);
        assertThat(seats).allSatisfy(s -> assertThat(s.getStatus()).isEqualTo("LOCKED"));
    }

    @Test
    void getSeats_shouldFilterByFareClassAndStatus() {
        UUID flightId = UUID.randomUUID();

        insertSeat(UUID.randomUUID(), flightId, "1A",  "BUSINESS","LOCKED",    "1500.00");
        insertSeat(UUID.randomUUID(), flightId, "1B",  "BUSINESS","AVAILABLE", "1400.00");
        insertSeat(UUID.randomUUID(), flightId, "12A", "ECONOMY", "LOCKED",    "300.00");

        List<SeatResponse> seats = repo.getSeats(flightId, "BUSINESS", "LOCKED");

        assertThat(seats).hasSize(1);
        assertThat(seats.get(0).getFareClass()).isEqualTo("BUSINESS");
        assertThat(seats.get(0).getStatus()).isEqualTo("LOCKED");
        assertThat(seats.get(0).getSeatNumber()).isEqualTo("1A");
    }

    @Test
    void getSeats_shouldReturnEmpty_whenNoMatches() {
        UUID flightId = UUID.randomUUID();
        insertSeat(UUID.randomUUID(), flightId, "12A", "ECONOMY", "AVAILABLE", "300.00");

        List<SeatResponse> seats = repo.getSeats(flightId, "BUSINESS", "LOCKED");
        assertThat(seats).isEmpty();
    }

    private void insertSeat(UUID id, UUID flightId, String seatNumber, String fareClass, String status, String price) {
        jdbcTemplate.update("""
            INSERT INTO seat_inventory (id, flight_id, seat_number, fare_class, status, price)
            VALUES (?, ?, ?, ?, ?, ?)
        """, id, flightId, seatNumber, fareClass, status, new BigDecimal(price));
    }
}
