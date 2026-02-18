package com.airline.flighmngmt.repository;

import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.repository.FlightQueryJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightQueryJdbcRepositoryTest {

    @InjectMocks
    private FlightQueryJdbcRepository flightQueryJdbcRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private String origin;
    private String destination;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        origin = "NYC";
        destination = "LAX";
        date = LocalDate.of(2024, 1, 1);
    }

    @Test
    void search_successful_returnsFlightSearchResponses() {
        // Arrange
        UUID flightId1 = UUID.randomUUID();
        UUID flightId2 = UUID.randomUUID();
        OffsetDateTime departureTime1 = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime arrivalTime1 = departureTime1.plusHours(5);
        OffsetDateTime departureTime2 = departureTime1.plusHours(2);
        OffsetDateTime arrivalTime2 = departureTime2.plusHours(5);

        List<FlightSearchResponse> expected = List.of(
                FlightSearchResponse.builder()
                        .flightId(flightId1)
                        .flightNumber("FL123")
                        .departureTime(departureTime1)
                        .arrivalTime(arrivalTime1)
                        .status("SCHEDULED")
                        .basePrice(BigDecimal.valueOf(200.00))
                        .build(),
                FlightSearchResponse.builder()
                        .flightId(flightId2)
                        .flightNumber("FL456")
                        .departureTime(departureTime2)
                        .arrivalTime(arrivalTime2)
                        .status("DELAYED")
                        .basePrice(BigDecimal.valueOf(250.00))
                        .build()
        );

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(expected);

        // Act
        List<FlightSearchResponse> actual = flightQueryJdbcRepository.search(origin, destination, date);

        // Assert
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertEquals(expected.get(0).getFlightId(), actual.get(0).getFlightId());
        assertEquals(expected.get(0).getFlightNumber(), actual.get(0).getFlightNumber());
        assertEquals(expected.get(0).getDepartureTime(), actual.get(0).getDepartureTime());
        assertEquals(expected.get(0).getArrivalTime(), actual.get(0).getArrivalTime());
        assertEquals(expected.get(0).getStatus(), actual.get(0).getStatus());
        assertEquals(expected.get(0).getBasePrice(), actual.get(0).getBasePrice());

        // Verify SQL and params
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        ArgumentCaptor<RowMapper> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), mapperCaptor.capture());

        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("SELECT f.id, f.flight_number, f.departure_time, f.arrival_time, f.status, f.base_price"));
        assertTrue(capturedSql.contains("FROM flights f"));
        assertTrue(capturedSql.contains("JOIN routes r ON r.id = f.route_id"));
        assertTrue(capturedSql.contains("WHERE r.origin_airport = :origin"));
        assertTrue(capturedSql.contains("AND r.destination_airport = :destination"));
        assertTrue(capturedSql.contains("AND f.departure_time >= :fromTs"));
        assertTrue(capturedSql.contains("AND f.departure_time <  :toTs"));
        assertTrue(capturedSql.contains("ORDER BY f.departure_time"));

        MapSqlParameterSource capturedParams = paramsCaptor.getValue();
        assertEquals(origin, capturedParams.getValue("origin"));
        assertEquals(destination, capturedParams.getValue("destination"));
        OffsetDateTime expectedFromTs = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedToTs = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        assertEquals(expectedFromTs, capturedParams.getValue("fromTs"));
        assertEquals(expectedToTs, capturedParams.getValue("toTs"));
    }

    @Test
    void search_noResults_returnsEmptyList() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        // Act
        List<FlightSearchResponse> actual = flightQueryJdbcRepository.search(origin, destination, date);

        // Assert
        assertTrue(actual.isEmpty());

        verify(jdbcTemplate).query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class));
    }
}