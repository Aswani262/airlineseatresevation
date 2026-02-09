package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.Aircraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AircraftJdbcRepository implements AircraftRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AircraftJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(Aircraft a) {
        String sql = """
            INSERT INTO aircraft (
                id,
                registration_number,
                model,
                manufacturer,
                total_seats,
                configuration
            )
            VALUES (
                :id,
                :registrationNumber,
                :model,
                :manufacturer,
                :totalSeats,
                CAST(:configuration AS jsonb)
            )
        """;

        var params = new MapSqlParameterSource()
                .addValue("id", a.getId())
                .addValue("registrationNumber", a.getRegistrationNumber())
                .addValue("model", a.getModel())
                .addValue("manufacturer", a.getManufacturer())
                .addValue("totalSeats", a.getTotalSeats())
                .addValue("configuration", toJson(a.getConfiguration()));

        try {
            jdbcTemplate.update(sql, params);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Aircraft with this registration number already exists", e);
        }
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        String sql = """
            SELECT COUNT(*)
            FROM aircraft
            WHERE registration_number = :registrationNumber
        """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("registrationNumber", registrationNumber),
                Integer.class
        );

        return count != null && count > 0;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize configuration to JSON", e);
        }
    }
}
