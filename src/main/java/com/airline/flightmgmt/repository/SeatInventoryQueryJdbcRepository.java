package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class SeatInventoryQueryJdbcRepository implements SeatInventoryQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SeatInventoryQueryJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SeatAvailabilitySummaryResponse getSummary(UUID flightId) {
        String sql = """
            SELECT fare_class,
                   COUNT(*) FILTER (WHERE status='AVAILABLE') AS available,
                   COUNT(*) FILTER (WHERE status='LOCKED') AS locked,
                   COUNT(*) FILTER (WHERE status='BOOKED') AS booked,
                   MIN(price) FILTER (WHERE status='AVAILABLE') AS min_price
            FROM seat_inventory
            WHERE flight_id = :flightId
            GROUP BY fare_class
            ORDER BY fare_class
        """;

        var params = new MapSqlParameterSource()
                .addValue("flightId", flightId);

        List<SeatAvailabilitySummaryResponse.FareClassAvailability> rows =
                jdbcTemplate.query(sql, params, (rs, rowNum) ->
                        SeatAvailabilitySummaryResponse.FareClassAvailability.builder()
                                .fareClass(rs.getString("fare_class"))
                                .available(rs.getLong("available"))
                                .locked(rs.getLong("locked"))
                                .booked(rs.getLong("booked"))
                                .minPrice(rs.getBigDecimal("min_price"))
                                .build()
                );

        return SeatAvailabilitySummaryResponse.builder()
                .flightId(flightId)
                .availability(rows)
                .build();
    }

    @Override
    public List<SeatResponse> getSeats(UUID flightId, String fareClass, String status) {
        String sql = """
        SELECT id AS seat_id, seat_number, fare_class, status, price
        FROM seat_inventory
        WHERE flight_id = :flightId
          AND (:fareClass::text IS NULL OR fare_class = :fareClass::text)
          AND (:status::text   IS NULL OR status    = :status::text)
        ORDER BY fare_class, seat_number
    """;

        var params = new MapSqlParameterSource()
                .addValue("flightId", flightId)
                .addValue("fareClass", fareClass)
                .addValue("status", status);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> SeatResponse.builder()
                .seatId(rs.getObject("seat_id", UUID.class))
                .seatNumber(rs.getString("seat_number"))
                .fareClass(rs.getString("fare_class"))
                .status(rs.getString("status"))
                .price(rs.getBigDecimal("price"))
                .build());
    }

}
