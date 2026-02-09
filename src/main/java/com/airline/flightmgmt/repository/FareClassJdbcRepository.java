package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FareClassJdbcRepository implements FareClassRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FareClassJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FareClassResponse getByCode(String code) {
        String sql = """
            SELECT code, name, description, baggage_allowance_kg, carry_on_allowed,
                   refundable, changeable, change_fee_percentage,
                   priority_boarding, meal_service, seat_selection_free
            FROM fare_classes
            WHERE code = :code
        """;

        var params = new MapSqlParameterSource()
                .addValue("code", code);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> FareClassResponse.builder()
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .baggageAllowanceKg(rs.getInt("baggage_allowance_kg"))
                .carryOnAllowed(rs.getBoolean("carry_on_allowed"))
                .refundable(rs.getBoolean("refundable"))
                .changeable(rs.getBoolean("changeable"))
                .changeFeePercentage(rs.getBigDecimal("change_fee_percentage"))
                .priorityBoarding(rs.getBoolean("priority_boarding"))
                .mealService(rs.getBoolean("meal_service"))
                .seatSelectionFree(rs.getBoolean("seat_selection_free"))
                .build());
    }

//    @Override
//    public Optional<FareClassResponse> getByCode(String code) {
//        try {
//            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
//        } catch (EmptyResultDataAccessException e) {
//            return Optional.empty();
//        }
//    }

}
