package com.airline.flighmngmt.repository;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.repository.FareClassJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FareClassJdbcRepositoryTest {

    @InjectMocks
    private FareClassJdbcRepository fareClassJdbcRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private String code;

    @BeforeEach
    void setUp() {
        code = "ECONOMY";
    }

    @Test
    void getByCode_successful_returnsFareClassResponse() {
        // Arrange
        FareClassResponse expected = FareClassResponse.builder()
                .code("ECONOMY")
                .name("Economy Class")
                .description("Standard economy seating")
                .baggageAllowanceKg(23)
                .carryOnAllowed(true)
                .refundable(false)
                .changeable(true)
                .changeFeePercentage(BigDecimal.valueOf(10.0))
                .priorityBoarding(false)
                .mealService(false)
                .seatSelectionFree(false)
                .build();

        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(expected);

        // Act
        FareClassResponse actual = fareClassJdbcRepository.getByCode(code);

        // Assert
        assertNotNull(actual);
        assertEquals(expected.getCode(), actual.getCode());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getBaggageAllowanceKg(), actual.getBaggageAllowanceKg());
        assertEquals(expected.getCarryOnAllowed(), actual.getCarryOnAllowed());
        assertEquals(expected.getRefundable(), actual.getRefundable());
        assertEquals(expected.getChangeable(), actual.getChangeable());
        assertEquals(expected.getChangeFeePercentage(), actual.getChangeFeePercentage());
        assertEquals(expected.getPriorityBoarding(), actual.getPriorityBoarding());
        assertEquals(expected.getMealService(), actual.getMealService());
        assertEquals(expected.getSeatSelectionFree(), actual.getSeatSelectionFree());

        // Verify SQL and params
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        ArgumentCaptor<RowMapper> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);

        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), mapperCaptor.capture());

        String capturedSql = sqlCaptor.getValue();
        assertTrue(capturedSql.contains("SELECT code, name, description"));
        assertTrue(capturedSql.contains("FROM fare_classes"));
        assertTrue(capturedSql.contains("WHERE code = :code"));

        MapSqlParameterSource capturedParams = paramsCaptor.getValue();
        assertEquals(code, capturedParams.getValue("code"));
    }

    @Test
    void getByCode_noResult_throwsEmptyResultDataAccessException() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(EmptyResultDataAccessException.class);

        // Act & Assert
        assertThrows(EmptyResultDataAccessException.class, () -> fareClassJdbcRepository.getByCode(code));

        verify(jdbcTemplate).queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class));
    }
}