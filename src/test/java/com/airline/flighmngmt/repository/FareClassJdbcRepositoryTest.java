package com.airline.flighmngmt.repository;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.repository.FareClassJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(FareClassJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class FareClassJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired
    FareClassJdbcRepository repo;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("""
            DROP TABLE IF EXISTS fare_classes;

            CREATE TABLE fare_classes (
                code VARCHAR(50) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description VARCHAR(500),

                baggage_allowance_kg INT,
                carry_on_allowed BOOLEAN,

                refundable BOOLEAN,
                changeable BOOLEAN,
                change_fee_percentage DECIMAL(10,2),

                priority_boarding BOOLEAN,
                meal_service BOOLEAN,
                seat_selection_free BOOLEAN
            );
        """);

        jdbcTemplate.update("""
            INSERT INTO fare_classes (
                code, name, description,
                baggage_allowance_kg, carry_on_allowed,
                refundable, changeable, change_fee_percentage,
                priority_boarding, meal_service, seat_selection_free
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                "ECONOMY",
                "Economy Saver",
                "Basic economy class",
                15,
                true,
                false,
                false,
                new BigDecimal("10.50"),
                false,
                false,
                false
        );
    }

    @Test
    void getByCode_shouldReturnFareClass_whenExists() {
        FareClassResponse res = repo.getByCode("ECONOMY");

        assertThat(res).isNotNull();
        assertThat(res.getCode()).isEqualTo("ECONOMY");
        assertThat(res.getName()).isEqualTo("Economy Saver");
        assertThat(res.getDescription()).isEqualTo("Basic economy class");

        assertThat(res.getBaggageAllowanceKg()).isEqualTo(15);
        assertThat(res.getCarryOnAllowed()).isTrue();

        assertThat(res.getRefundable()).isFalse();
        assertThat(res.getChangeable()).isFalse();
        assertThat(res.getChangeFeePercentage()).isEqualByComparingTo("10.50");

        assertThat(res.getPriorityBoarding()).isFalse();
        assertThat(res.getMealService()).isFalse();
        assertThat(res.getSeatSelectionFree()).isFalse();
    }

    @Test
    void getByCode_shouldThrow_whenNotFound() {
        assertThatThrownBy(() -> repo.getByCode("BUSINESS"))
                .isInstanceOf(EmptyResultDataAccessException.class);
    }

    @Test
    void getByCode_shouldPassExactCodeParam() {
        // this test ensures we don't accidentally uppercase/trim in repo layer (current code doesn't)
        // Insert another code with special casing
        jdbcTemplate.update("""
            INSERT INTO fare_classes (
                code, name, description,
                baggage_allowance_kg, carry_on_allowed,
                refundable, changeable, change_fee_percentage,
                priority_boarding, meal_service, seat_selection_free
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                "eco_plus",
                "Eco Plus",
                "Mixed case code",
                20, true,
                true, true, new BigDecimal("0.00"),
                true, true, true
        );

        FareClassResponse res = repo.getByCode("eco_plus");
        assertThat(res.getCode()).isEqualTo("eco_plus");

        // sanity check row exists as-is
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM fare_classes WHERE code = ?", "eco_plus");
        assertThat(row.get("code")).isEqualTo("eco_plus");
    }
}
