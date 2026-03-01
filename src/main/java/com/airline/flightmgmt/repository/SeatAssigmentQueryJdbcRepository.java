package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.api.dto.SeatAssignmentsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SeatAssigmentQueryJdbcRepository implements SeatAssigmentQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    @Override
    public List<SeatAssignmentsResponse> getAssignedSeats(UUID flightId, LocalDate flightDate) {
        String sql = "SELECT seat_template_id AS seatTemplateId, status " +
                "FROM seat_assignments " +
                "WHERE flight_id = :flightId AND flight_date = :flightDate";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("flightId", flightId)
                .addValue("flightDate", flightDate, Types.DATE);

        return jdbcTemplate.query(sql, params, BeanPropertyRowMapper.newInstance(SeatAssignmentsResponse.class));
    }
}
