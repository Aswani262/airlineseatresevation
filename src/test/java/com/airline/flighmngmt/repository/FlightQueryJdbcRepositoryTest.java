package com.airline.flighmngmt.repository;

import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.repository.FlightQueryJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(FlightQueryJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class FlightQueryJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired FlightQueryJdbcRepository repo;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("""
            DROP TABLE IF EXISTS flights;
            DROP TABLE IF EXISTS routes;

            CREATE TABLE routes (
                id UUID PRIMARY KEY,
                origin_airport CHAR(3) NOT NULL,
                destination_airport CHAR(3) NOT NULL
            );

            CREATE TABLE flights (
                id UUID PRIMARY KEY,
                route_id UUID NOT NULL,
                flight_number VARCHAR(20) NOT NULL,
                departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
                arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
                status VARCHAR(20) NOT NULL,
                base_price DECIMAL(12,2) NOT NULL
            );
        """);
    }

    @Test
    void search_shouldReturnFlightsForOriginDestinationAndDate_inUtcWindow_sortedByDeparture() {
        // Given
        UUID routeBomDel = UUID.randomUUID();
        UUID routeBomBlr = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO routes (id, origin_airport, destination_airport)
            VALUES (?, ?, ?)
        """, routeBomDel, "BOM", "DEL");

        jdbcTemplate.update("""
            INSERT INTO routes (id, origin_airport, destination_airport)
            VALUES (?, ?, ?)
        """, routeBomBlr, "BOM", "BLR");

        LocalDate date = LocalDate.of(2026, 2, 8);

        // flights in BOM->DEL on the date (should match, sorted)
        UUID f1 = UUID.randomUUID();
        UUID f2 = UUID.randomUUID();

        OffsetDateTime dep1 = OffsetDateTime.of(2026, 2, 8, 6, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime arr1 = OffsetDateTime.of(2026, 2, 8, 8, 0, 0, 0, ZoneOffset.UTC);

        OffsetDateTime dep2 = OffsetDateTime.of(2026, 2, 8, 9, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime arr2 = OffsetDateTime.of(2026, 2, 8, 11, 30, 0, 0, ZoneOffset.UTC);

        insertFlight(f2, routeBomDel, "AI-200", dep2, arr2, "SCHEDULED", new BigDecimal("2500.00"));
        insertFlight(f1, routeBomDel, "AI-100", dep1, arr1, "DELAYED", new BigDecimal("1500.00"));

        // flight in BOM->DEL previous day (excluded)
        insertFlight(UUID.randomUUID(), routeBomDel, "AI-090",
                OffsetDateTime.of(2026, 2, 7, 23, 59, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 2, 8, 1, 0, 0, 0, ZoneOffset.UTC),
                "SCHEDULED", new BigDecimal("999.00"));

        // flight in BOM->DEL next day exactly at boundary (excluded, because < toTs)
        insertFlight(UUID.randomUUID(), routeBomDel, "AI-300",
                OffsetDateTime.of(2026, 2, 9, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 2, 9, 2, 0, 0, 0, ZoneOffset.UTC),
                "SCHEDULED", new BigDecimal("3000.00"));

        // wrong route BOM->BLR same day (excluded)
        insertFlight(UUID.randomUUID(), routeBomBlr, "AI-777",
                OffsetDateTime.of(2026, 2, 8, 7, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 2, 8, 9, 0, 0, 0, ZoneOffset.UTC),
                "SCHEDULED", new BigDecimal("1800.00"));

        // When
        List<FlightSearchResponse> results = repo.search("BOM", "DEL", date);

        // Then
        assertThat(results).hasSize(2);

        // must be ordered by departure_time
        assertThat(results.get(0).getFlightId()).isEqualTo(f1);
        assertThat(results.get(0).getFlightNumber()).isEqualTo("AI-100");
        assertThat(results.get(0).getDepartureTime()).isEqualTo(dep1);
        assertThat(results.get(0).getArrivalTime()).isEqualTo(arr1);
        assertThat(results.get(0).getStatus()).isEqualTo("DELAYED");
        assertThat(results.get(0).getBasePrice()).isEqualByComparingTo("1500.00");

        assertThat(results.get(1).getFlightId()).isEqualTo(f2);
        assertThat(results.get(1).getFlightNumber()).isEqualTo("AI-200");
        assertThat(results.get(1).getDepartureTime()).isEqualTo(dep2);
        assertThat(results.get(1).getArrivalTime()).isEqualTo(arr2);
        assertThat(results.get(1).getStatus()).isEqualTo("SCHEDULED");
        assertThat(results.get(1).getBasePrice()).isEqualByComparingTo("2500.00");
    }

    @Test
    void search_shouldReturnEmpty_whenNoMatches() {
        // Given (no routes/flights inserted)
        List<FlightSearchResponse> results = repo.search("BOM", "DEL", LocalDate.of(2026, 2, 8));

        assertThat(results).isEmpty();
    }

    private void insertFlight(
            UUID id,
            UUID routeId,
            String flightNumber,
            OffsetDateTime departure,
            OffsetDateTime arrival,
            String status,
            BigDecimal basePrice
    ) {
        jdbcTemplate.update("""
            INSERT INTO flights (id, route_id, flight_number, departure_time, arrival_time, status, base_price)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, id, routeId, flightNumber, departure, arrival, status, basePrice);
    }
}
