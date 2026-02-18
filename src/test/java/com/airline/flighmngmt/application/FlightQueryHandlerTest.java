package com.airline.flighmngmt.application;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import com.airline.flightmgmt.application.query.FlightQueryHandler;
import com.airline.flightmgmt.repository.FareClassRepository;
import com.airline.flightmgmt.repository.FlightQueryRepository;
import com.airline.flightmgmt.repository.SeatInventoryQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightQueryHandlerTest {

    @InjectMocks
    private FlightQueryHandler flightQueryHandler;

    @Mock
    private FlightQueryRepository flightQueryRepository;

    @Mock
    private SeatInventoryQueryRepository seatInventoryQueryRepository;

    @Mock
    private FareClassRepository fareClassRepository;

    private UUID flightId;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();
        date = LocalDate.now();
    }

    @Test
    void search_trimsAndUppercasesParams_returnsList() {
        // Arrange
        String origin = " nyc ";
        String destination = " lax ";
        List<FlightSearchResponse> expected = List.of(
                FlightSearchResponse.builder()
                        .flightId(UUID.randomUUID())
                        .flightNumber("FL123")
                        .departureTime(OffsetDateTime.now())
                        .arrivalTime(OffsetDateTime.now().plusHours(5))
                        .status("SCHEDULED")
                        .basePrice(BigDecimal.valueOf(200))
                        .build()
        );

        when(flightQueryRepository.search("NYC", "LAX", date)).thenReturn(expected);

        // Act
        List<FlightSearchResponse> actual = flightQueryHandler.search(origin, destination, date);

        // Assert
        assertEquals(expected, actual);
        verify(flightQueryRepository).search("NYC", "LAX", date);
    }

    @Test
    void getSummary_callsRepository_returnsResponse() {
        // Arrange
        SeatAvailabilitySummaryResponse expected = SeatAvailabilitySummaryResponse.builder()
                .flightId(flightId)
                .availability(List.of(
                        SeatAvailabilitySummaryResponse.FareClassAvailability.builder()
                                .fareClass("ECONOMY")
                                .available(10)
                                .locked(2)
                                .booked(5)
                                .minPrice(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();

        when(seatInventoryQueryRepository.getSummary(flightId)).thenReturn(expected);

        // Act
        SeatAvailabilitySummaryResponse actual = flightQueryHandler.getSummary(flightId);

        // Assert
        assertEquals(expected, actual);
        verify(seatInventoryQueryRepository).getSummary(flightId);
    }

    @Test
    void getSeats_withParams_trimsAndUppercases_returnsList() {
        // Arrange
        String fareClass = " economy ";
        String status = " available ";
        List<SeatResponse> expected = List.of(
                SeatResponse.builder()
                        .seatId(UUID.randomUUID())
                        .seatNumber("A1")
                        .fareClass("ECONOMY")
                        .status("AVAILABLE")
                        .price(BigDecimal.valueOf(100))
                        .build()
        );

        when(seatInventoryQueryRepository.getSeats(flightId, "ECONOMY", "AVAILABLE")).thenReturn(expected);

        // Act
        List<SeatResponse> actual = flightQueryHandler.getSeats(flightId, fareClass, status);

        // Assert
        assertEquals(expected, actual);
        verify(seatInventoryQueryRepository).getSeats(flightId, "ECONOMY", "AVAILABLE");
    }

    @Test
    void getSeats_nullParams_passesNull_returnsList() {
        // Arrange
        List<SeatResponse> expected = List.of();

        when(seatInventoryQueryRepository.getSeats(flightId, null, null)).thenReturn(expected);

        // Act
        List<SeatResponse> actual = flightQueryHandler.getSeats(flightId, null, null);

        // Assert
        assertEquals(expected, actual);
        verify(seatInventoryQueryRepository).getSeats(flightId, null, null);
    }

    @Test
    void getByCode_trimsAndUppercases_returnsResponse() {
        // Arrange
        String code = " economy ";
        FareClassResponse expected = FareClassResponse.builder()
                .code("ECONOMY")
                .name("Economy")
                .description("Standard class")
                .baggageAllowanceKg(23)
                .carryOnAllowed(true)
                .refundable(false)
                .changeable(true)
                .changeFeePercentage(BigDecimal.valueOf(10))
                .priorityBoarding(false)
                .mealService(false)
                .seatSelectionFree(false)
                .build();

        when(fareClassRepository.getByCode("ECONOMY")).thenReturn(expected);

        // Act
        FareClassResponse actual = flightQueryHandler.getByCode(code);

        // Assert
        assertEquals(expected, actual);
        verify(fareClassRepository).getByCode("ECONOMY");
    }
}