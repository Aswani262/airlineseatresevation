package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class FlightQueryJdbcRepository implements FlightQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FlightQueryJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FlightSearchResponse> search(String origin, String destination, LocalDate date) {
        String sql = """
            SELECT f.id, f.flight_number, f.departure_time, f.arrival_time, f.status, f.base_price
            FROM flights f
            JOIN routes r ON r.id = f.route_id
            WHERE r.origin_airport = :origin
              AND r.destination_airport = :destination
              AND f.departure_time >= :fromTs
              AND f.departure_time <  :toTs
            ORDER BY f.departure_time
        """;

        // If your DB stores timestamptz in UTC (recommended), this is good.
        // If you store times in a specific local timezone, adjust ZoneOffset accordingly.
        OffsetDateTime fromTs = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        var params = new MapSqlParameterSource()
                .addValue("origin", origin)
                .addValue("destination", destination)
                .addValue("fromTs", fromTs)
                .addValue("toTs", toTs);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> FlightSearchResponse.builder()
                .flightId(rs.getObject("id", UUID.class))
                .flightNumber(rs.getString("flight_number"))
                .departureTime(rs.getObject("departure_time", OffsetDateTime.class))
                .arrivalTime(rs.getObject("arrival_time", OffsetDateTime.class))
                .status(rs.getString("status"))
                .basePrice(rs.getBigDecimal("base_price"))
                .build()
        );
    }
}
